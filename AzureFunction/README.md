# Azure Function: auto zip/unzip blobs

This Function App watches the `input` blob container. When a blob is created or updated:

- **`.zip` files** are extracted into `output/{zipName}/...`
- **Any other file** is wrapped in a zip and written to `output/{originalName}.zip`

The function writes only to `output`, so it cannot retrigger itself.

## Examples

| Upload to `input` | Result in `output` |
| --- | --- |
| `photos.zip` containing `img1.jpg` and `folder/img2.png` | `photos/img1.jpg`, `photos/folder/img2.png` |
| `report.pdf` | `report.pdf.zip` |

Each loose file is zipped on its own. Blob storage has no real folders, so uploading several files does not produce one combined archive.

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local)
- Either [Azurite](https://learn.microsoft.com/azure/storage/common/storage-use-azurite) (local emulator) or a real Azure Storage connection string

## Local setup

1. Copy `local.settings.json` if you removed it (it is gitignored after the first clone). It should contain:

```json
{
  "IsEncrypted": false,
  "Values": {
    "AzureWebJobsStorage": "UseDevelopmentStorage=true",
    "FUNCTIONS_WORKER_RUNTIME": "dotnet-isolated"
  }
}
```

For a real storage account, replace `UseDevelopmentStorage=true` with the account connection string from the Azure portal (Storage account → Access keys).

2. Start Azurite if you are using the emulator:

```powershell
azurite --silent --location $env:TEMP\azurite --debug $env:TEMP\azurite-debug.log
```

3. Restore, build, and run from this folder:

```powershell
dotnet restore
dotnet build
func start
```

On startup the function creates the `input` and `output` containers if they do not already exist.

## How to test

Upload a blob to the `input` container, then check `output`.

**Azure Storage Explorer / portal:** upload `notes.txt` or `archive.zip` into `input`.

**Azure CLI:**

```powershell
az storage blob upload --account-name <storage-account> --container-name input --name notes.txt --file .\notes.txt --auth-mode key
az storage blob upload --account-name <storage-account> --container-name input --name photos.zip --file .\photos.zip --auth-mode key
```

**Azurite with Azure CLI:**

```powershell
az storage blob upload --connection-string "UseDevelopmentStorage=true" --container-name input --name notes.txt --file .\notes.txt
```

You should see log lines such as:

- `Zipping blob notes.txt ...` then `Finished zipping notes.txt to notes.txt.zip.`
- `Unzipping blob photos.zip ...` then `Extracted ... to photos/...`

## Deploy to Azure

1. Create a Function App (Consumption, Flex, or App Service) on .NET 8 isolated, and a storage account.
2. Set the app setting `AzureWebJobsStorage` to that storage account's connection string.
3. Publish:

```powershell
func azure functionapp publish <your-function-app-name>
```

The first run creates `input` and `output` if they are missing. You can also create those containers in the portal beforehand.

## Project layout

- `Program.cs` — host startup and container creation
- `Functions/ProcessBlobFunction.cs` — blob trigger, zip, and unzip
- `host.json` — Functions host configuration
- `local.settings.json` — local connection strings (not published)
