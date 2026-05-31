# task-service/Dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl task-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /workspace/task-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
