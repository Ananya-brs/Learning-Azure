using System.IO.Compression;
using Azure.Storage.Blobs;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Extensions.Logging;

namespace ZipUnzipFunction;

public sealed class ProcessBlobFunction
{
    private readonly BlobServiceClient _blobServiceClient;
    private readonly ILogger<ProcessBlobFunction> _logger;

    public ProcessBlobFunction(
        BlobServiceClient blobServiceClient,
        ILogger<ProcessBlobFunction> logger)
    {
        _blobServiceClient = blobServiceClient;
        _logger = logger;
    }

    [Function(nameof(ProcessBlob))]
    public async Task ProcessBlob(
        [BlobTrigger("input/{name}", Connection = "AzureWebJobsStorage")] Stream blob,
        string name,
        CancellationToken cancellationToken)
    {
        if (string.IsNullOrWhiteSpace(name))
        {
            _logger.LogWarning("Skipped blob with an empty name.");
            return;
        }

        var outputContainer = _blobServiceClient.GetBlobContainerClient(StorageContainers.Output);

        if (name.EndsWith(".zip", StringComparison.OrdinalIgnoreCase))
        {
            _logger.LogInformation("Unzipping blob {BlobName} from {InputContainer} into {OutputContainer}.",
                name, StorageContainers.Input, StorageContainers.Output);
            await UnzipToOutputAsync(blob, name, outputContainer, cancellationToken);
            return;
        }

        _logger.LogInformation("Zipping blob {BlobName} from {InputContainer} into {OutputContainer}.",
            name, StorageContainers.Input, StorageContainers.Output);
        await ZipToOutputAsync(blob, name, outputContainer, cancellationToken);
    }

    private async Task UnzipToOutputAsync(
        Stream blob,
        string name,
        BlobContainerClient outputContainer,
        CancellationToken cancellationToken)
    {
        var archiveFolder = Path.GetFileNameWithoutExtension(name);
        if (string.IsNullOrWhiteSpace(archiveFolder))
        {
            _logger.LogWarning("Could not determine an output folder for zip blob {BlobName}.", name);
            return;
        }

        var tempPath = Path.GetTempFileName();
        try
        {
            await using (var tempWrite = File.Create(tempPath))
            {
                await blob.CopyToAsync(tempWrite, cancellationToken);
            }

            await using var zipStream = File.OpenRead(tempPath);
            using var archive = new ZipArchive(zipStream, ZipArchiveMode.Read);

            var extractedCount = 0;
            foreach (var entry in archive.Entries)
            {
                var relativePath = GetSafeRelativePath(entry.FullName);
                if (relativePath is null)
                {
                    continue;
                }

                var destinationName = $"{archiveFolder}/{relativePath}";
                var outputBlob = outputContainer.GetBlobClient(destinationName);

                await using var entryStream = entry.Open();
                await outputBlob.UploadAsync(entryStream, overwrite: true, cancellationToken);

                extractedCount++;
                _logger.LogInformation("Extracted {EntryName} to {DestinationBlob}.", entry.FullName, destinationName);
            }

            _logger.LogInformation("Finished unzipping {BlobName}. Extracted {Count} file(s) under {DestinationPrefix}.",
                name, extractedCount, archiveFolder);
        }
        finally
        {
            TryDeleteTempFile(tempPath);
        }
    }

    private async Task ZipToOutputAsync(
        Stream blob,
        string name,
        BlobContainerClient outputContainer,
        CancellationToken cancellationToken)
    {
        var destinationName = $"{name}.zip";
        var outputBlob = outputContainer.GetBlobClient(destinationName);
        var entryName = Path.GetFileName(name.Replace('\\', '/'));
        if (string.IsNullOrWhiteSpace(entryName))
        {
            entryName = "file";
        }

        var tempPath = Path.GetTempFileName();
        try
        {
            await using (var zipFile = File.Create(tempPath))
            {
                using (var archive = new ZipArchive(zipFile, ZipArchiveMode.Create, leaveOpen: true))
                {
                    var entry = archive.CreateEntry(entryName, CompressionLevel.Optimal);
                    await using var entryStream = entry.Open();
                    await blob.CopyToAsync(entryStream, cancellationToken);
                }
            }

            await using var zipRead = File.OpenRead(tempPath);
            await outputBlob.UploadAsync(zipRead, overwrite: true, cancellationToken);

            _logger.LogInformation("Finished zipping {BlobName} to {DestinationBlob}.", name, destinationName);
        }
        finally
        {
            TryDeleteTempFile(tempPath);
        }
    }

    /// <summary>
    /// Rejects directory entries and path traversal so zip contents stay under the archive prefix.
    /// </summary>
    private static string? GetSafeRelativePath(string entryName)
    {
        if (string.IsNullOrWhiteSpace(entryName))
        {
            return null;
        }

        var normalized = entryName.Replace('\\', '/').Trim();
        if (normalized.EndsWith('/'))
        {
            return null;
        }

        var parts = normalized.Split('/', StringSplitOptions.RemoveEmptyEntries);
        if (parts.Length == 0 || parts.Any(part => part == "." || part == ".."))
        {
            return null;
        }

        return string.Join('/', parts);
    }

    private void TryDeleteTempFile(string path)
    {
        try
        {
            if (File.Exists(path))
            {
                File.Delete(path);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Could not delete temp file {TempPath}.", path);
        }
    }
}
