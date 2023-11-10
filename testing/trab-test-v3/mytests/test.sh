#!/bin/sh

for file in users/*; do
    curl -X POST -H "Content-Type: application/json" --data @"$file" https://scc24appwesteurope188046.azurewebsites.net/rest/user
done

for file in houses/*; do
    curl -X POST -H "Content-Type: application/json" --data @"$file" https://scc24appwesteurope188046.azurewebsites.net/rest/house
done
