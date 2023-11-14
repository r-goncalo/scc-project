mvn -f northeurope-pom.xml clean compile package azure-webapp:deploy
mvn -f westeurope-pom.xml clean compile package azure-webapp:deploy
read