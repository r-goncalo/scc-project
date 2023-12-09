FROM tomcat:10.0-jdk17-openjdk
RUN apt-get update && apt-get install -y dnsutils iputils-ping
WORKDIR /usr/local/tomcat
ADD ./../../project/target/scc2324-project1-1.0.war webapps
EXPOSE 8080




