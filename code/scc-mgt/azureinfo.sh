az functionapp list --query "[].{name : "name", hostName: defaultHostName, state: state, "rg": "resourceGroup"}"
read