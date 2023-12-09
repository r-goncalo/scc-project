#!/bin/sh
NUM="59895"
RESOURCE_GRP="scc2324-cluster-$NUM"
IMG_NAME="afonsorib/scc2324-app"
CLUSTER_NAME="my-scc2324-cluster-$NUM"

# create resource group
response="$(az group create --name "$RESOURCE_GRP" --location westeurope)"
echo resource group created!

scope="$(echo $response | jq .id )"

# create container
#az container create --resource-group "$RESOURCE_GRP" --name scc-app --image "$IMG_NAME" --ports 8080 --dns-name-label scc-reservation-59895

# link to project: cc-reservation-59895.westeurope.azurecontainer.io:8080/scc2324-project1-1.0
# todo print link with RESOURCE_GRP + IMG_NAME

#delete
#az container delete --resource-group "$RESOURCE_GRP" --name scc-app

#create a service principal
response2="$(az ad sp create-for-rbac --name http://scc2324-kuber-$NUM --role Contributor --scope $scope)"
echo service principal created!

appid="$(echo $response2 | jq .appID )"
password="$(echo $response2 | jq .password )"

az aks create --resource-group "$RESOURCE_GRP" --name "$CLUSTER_NAME" --node-vm-size Standard_B2s --generate-ssh-keys --node-count 2 --service-principal "$appid" --client-secret "$password"
echo cluster created!

az aks get-credentials --resource-group "$RESOURCE_GRP" --name "$CLUSTER_NAME"
echo credential obtained!

kubectl apply -f azure-vote.yaml
#kubectl apply -f configmap.yaml
#kubectl apply -f myapp.yaml


# apply rest of the things in the slides