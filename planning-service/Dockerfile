# planning-service/Dockerfile
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY . .
RUN mvn -pl planning-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /workspace/planning-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
