# Simple ASP.NET Core app for Azure App Service

A minimal **ASP.NET Core 8 Razor Pages** site you can run locally and deploy to Azure App Service (Linux, Free **F1**).

The home page shows **Hello from Azure App Service** and the current environment name (`Development` locally, `Production` on Azure).

The app name must be **globally unique**. It becomes the URL `https://<app-name>.azurewebsites.net`.

## Resource model

A **resource group** holds an **App Service plan** (compute and pricing) and a **Web App** (the site). Deleting the resource group removes everything.

```
Local Razor Pages app
        |
        v
  dotnet publish + ZIP
        |
        v
  App Service Plan (F1 Linux) --> Web App --> https://<app-name>.azurewebsites.net
```

## Prerequisites

- An Azure subscription ([free account](https://azure.microsoft.com/pricing/purchase-options/azure-account))
- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Azure CLI](https://learn.microsoft.com/cli/azure/install-azure-cli)

Confirm tools:

```powershell
dotnet --version
az --version
```

## Run locally

From this folder:

```powershell
dotnet restore
dotnet run
```

Open the URL shown in the terminal (typically `https://localhost:7xxx` or `http://localhost:5xxx`). You should see **Hello from Azure App Service** and environment **Development**.

## Complete Azure Portal steps

1. Open the [Azure Portal](https://portal.azure.com) and search **Web App** → **Create**.
2. On **Basics**:
   - **Subscription:** yours
   - **Resource group:** create `rg-simple-webapp`
   - **Name:** unique, for example `simple-webapp-<yourname>`
   - **Publish:** Code
   - **Runtime stack:** .NET 8 (LTS)
   - **Operating System:** Linux
   - **Region:** closest to you (for example Central India or East US)
   - **Pricing plan:** Free F1 (create a new plan if needed)
3. Skip **Database** and **Deployment** for now. Select **Review + create**, then **Create**.
4. After the web app is deployed, open the resource and select **Browse**. You should see the default App Service page until you publish this project.
5. Deploy from your machine with the CLI zip deploy below, then **Browse** again.

Useful blades after deploy:

| Blade | Use |
| --- | --- |
| **Deployment Center** | Later CI/CD from GitHub |
| **Log stream** | Live stdout |
| **Diagnose and solve problems** | Startup failures |
| **Configuration** → Application settings | Environment variables (do not commit secrets) |

## Complete Azure CLI steps

Sign in:

```powershell
az login
```

Replace `simple-webapp-<unique>` with a name that is unique across all of Azure.

Create the resource group, Linux Free plan, and web app:

```powershell
az group create --name rg-simple-webapp --location eastus

az appservice plan create `
  --name plan-simple-webapp `
  --resource-group rg-simple-webapp `
  --sku F1 `
  --is-linux

az webapp create `
  --name simple-webapp-<unique> `
  --resource-group rg-simple-webapp `
  --plan plan-simple-webapp `
  --runtime "DOTNETCORE:8.0"
```

Linux App Service expects the **published** output, not the source project. From this folder:

```powershell
dotnet publish -c Release -o .\publish
Compress-Archive -Path .\publish\* -DestinationPath .\app.zip -Force

az webapp deploy `
  --resource-group rg-simple-webapp `
  --name simple-webapp-<unique> `
  --src-path .\app.zip `
  --type zip
```

### One-command alternative

Creates resources if needed (run from this folder):

```powershell
az webapp up --sku F1 --name simple-webapp-<unique> --os-type linux --runtime "DOTNETCORE:8.0"
```

## Verify

Open:

```text
https://simple-webapp-<unique>.azurewebsites.net
```

You should see **Hello from Azure App Service** and environment **Production**. The Free F1 plan can **cold-start** after idle time; wait a few seconds and refresh if the first request is slow.

Stream logs:

```powershell
az webapp log tail --name simple-webapp-<unique> --resource-group rg-simple-webapp
```

## Cleanup

F1 is free, but unused resource groups should still be deleted:

```powershell
az group delete --name rg-simple-webapp --yes --no-wait
```

## Learning material

### Do this first (hands-on, about 45–60 minutes)

- [Host a web application with Azure App Service](https://learn.microsoft.com/en-us/training/modules/host-a-web-app-with-azure-app-service/)
  - [Create a web app in the Azure portal](https://learn.microsoft.com/en-us/training/modules/host-a-web-app-with-azure-app-service/2-create-a-web-app-in-the-azure-portal)
  - [Deploy code to App Service](https://learn.microsoft.com/en-us/training/modules/host-a-web-app-with-azure-app-service/6-deploying-code-to-app-service)

### Official docs (reference while you work)

- [Quickstart: Deploy an ASP.NET web app](https://learn.microsoft.com/en-us/azure/app-service/quickstart-dotnetcore)
- [App Service overview](https://learn.microsoft.com/en-us/azure/app-service/overview)
- [Getting started hub](https://learn.microsoft.com/en-us/azure/app-service/getting-started)
- [Deploy files (ZIP)](https://learn.microsoft.com/en-us/azure/app-service/deploy-zip)
- [Configure ASP.NET Core on App Service](https://learn.microsoft.com/en-us/azure/app-service/configure-language-dotnetcore)
- [App Service plans / pricing](https://azure.microsoft.com/pricing/details/app-service/)

### Next (after the first successful deploy)

- [AZ-204: Implement Azure App Service web apps](https://learn.microsoft.com/en-us/training/paths/az-204-implement-azure-app-service-web-apps/)
- [Custom domain](https://learn.microsoft.com/en-us/azure/app-service/app-service-web-tutorial-custom-domain)
- [GitHub Actions deploy](https://learn.microsoft.com/en-us/azure/app-service/deploy-github-actions)
- [Diagnose and solve](https://learn.microsoft.com/en-us/azure/app-service/overview-diagnostics)

## Project layout

- `Program.cs` — ASP.NET Core host and Razor Pages pipeline
- `Pages/Index.cshtml` — home page
- `Pages/Index.cshtml.cs` — environment name for the home page
- `Pages/Shared/_Layout.cshtml` — site chrome
- `.gitignore` — ignores `bin/`, `obj/`, `publish/`, and `app.zip`
