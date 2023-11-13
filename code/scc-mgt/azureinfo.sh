echo "list of azure function apps:"
az functionapp list --query "[].{name : "name", hostName: defaultHostName, state: state, ResourceGroup: "resourceGroup"}"
echo "list of settings for functions: "
az functionapp config appsettings list --name scc24funwesteurope60519 --resource-group scc24-rg-westeurope-60519
echo "list of settings for app: "
az functionapp config appsettings list --name scc24appwesteurope60519 --resource-group scc24-rg-westeurope-60519
read