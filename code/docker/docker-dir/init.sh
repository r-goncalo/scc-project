#!/bin/sh
NUM="59895"
RESOURCE_GRP="scc2324-cluster-$NUM"
IMG_NAME="rgoncalo/scc2324-app"
CLUSTER_NAME="my-scc2324-cluster-$NUM"

# create resource group
response="$(az group create --name "$RESOURCE_GRP" --location westeurope)"
echo resource group created!

scope="$(echo $response | jq .id )"

#create a service principal
response2="$(az ad sp create-for-rbac --name http://scc2324-kuber-$NUM --role Contributor --scope $scope)"
echo service principal created!

appid="$(echo $response2 | jq .appID )"
password="$(echo $response2 | jq .password )"

az aks create --resource-group "$RESOURCE_GRP" --name "$CLUSTER_NAME" --node-vm-size Standard_B2s --generate-ssh-keys --node-count 2 --service-principal "$appid" --client-secret "$password"
echo cluster created!

az aks get-credentials --resource-group "$RESOURCE_GRP" --name "$CLUSTER_NAME"
echo credential obtained!


kubectl apply -f volume.yml
kubectl apply -f redis.yaml
kubectl apply -f scc2324-app.yaml
