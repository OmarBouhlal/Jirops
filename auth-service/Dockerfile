# auth-service/Dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY auth-service/pom.xml auth-service/
COPY jwt-commons/pom.xml jwt-commons/
COPY . .
RUN mvn -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
