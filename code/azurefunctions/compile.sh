mvn -f northeurope-pom.xml clean compile package azure-functions:deploy
mvn -f westeurope-pom.xml clean compile package azure-functions:deploy
read