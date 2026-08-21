using Azure.Storage.Blobs;
using Microsoft.Azure.Functions.Worker;
using Microsoft.Azure.Functions.Worker.Builder;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using ZipUnzipFunction;

var builder = FunctionsApplication.CreateBuilder(args);

builder.ConfigureFunctionsWebApplication();

builder.Services
    .AddApplicationInsightsTelemetryWorkerService()
    .ConfigureFunctionsApplicationInsights();

var connectionString = builder.Configuration["AzureWebJobsStorage"]
    ?? throw new InvalidOperationException("AzureWebJobsStorage is not configured.");

builder.Services.AddSingleton(_ => new BlobServiceClient(connectionString));

var host = builder.Build();

var blobServiceClient = host.Services.GetRequiredService<BlobServiceClient>();
await blobServiceClient.GetBlobContainerClient(StorageContainers.Input).CreateIfNotExistsAsync();
await blobServiceClient.GetBlobContainerClient(StorageContainers.Output).CreateIfNotExistsAsync();

await host.RunAsync();
