# task-service/Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl task-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/task-service/target/*.jar app.jar
FROM eclipse-temurin:17-jre

WORKDIR /app
COPY target/task-service-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
