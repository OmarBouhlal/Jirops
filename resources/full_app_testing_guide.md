# Jirops Full App Testing Guide

This guide covers local test commands, Docker startup, service health checks, and a copy-paste end-to-end API flow for the whole app.

## 0. Prerequisites

Install and verify:

```bash
java -version
mvn -version
docker --version
docker compose version
jq --version
```

`jq` is used to extract IDs and tokens from JSON responses. If it is missing, install it or copy values manually from the response bodies.

## 1. Clean Build and Automated Tests

Run the full Maven test suite from the repo root:

```bash
mvn clean test
```

Run tests for one module and its required dependencies:

```bash
mvn -pl jwt-commons test
mvn -pl auth-service -am test
mvn -pl project-service -am test
mvn -pl planning-service -am test
mvn -pl task-service -am test
```

Run focused test classes:

```bash
mvn -pl auth-service -am -Dtest=AuthServiceTest test
mvn -pl project-service -am -Dtest=ProjectServiceTest test
mvn -pl planning-service -am -Dtest=SprintServiceTest test
mvn -pl task-service -am -Dtest=TaskServiceTest test
mvn -pl task-service -am -Dtest=TaskEventConsumerKafkaIntegrationTest test
```

If a focused test command fails in another reactor module with `No tests matching pattern`, rerun with:

```bash
mvn -pl task-service -am -Dtest=TaskServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Build all service jars without tests:

```bash
mvn clean package -DskipTests
```

## 2. Start the Docker Stack

Start everything currently defined in `docker-compose.yml`, including `api-gateway`:

```bash
docker compose up --build -d
```

Watch startup logs:

```bash
docker compose logs -f eureka-server config-server auth-service project-service planning-service task-service api-gateway
```

Check container status:

```bash
docker compose ps
```

`api-gateway` is included in `docker-compose.yml` and is exposed on `http://localhost:8080`.

Watch only gateway logs:

```bash
docker compose logs -f api-gateway
```

Stop the full stack:

```bash
docker compose down
```

Reset all local databases and Kafka state:

```bash
docker compose down -v
```

## 3. Health, Discovery, and Config Checks

Health endpoints:

```bash
curl -i http://localhost:8761/actuator/health
curl -i http://localhost:8888/actuator/health
curl -i http://localhost:8081/actuator/health
curl -i http://localhost:8082/actuator/health
curl -i http://localhost:8083/actuator/health
curl -i http://localhost:8084/actuator/health
curl -i http://localhost:8080/actuator/health
```

Config Server responses:

```bash
curl -s http://localhost:8888/auth-service/default | jq .
curl -s http://localhost:8888/project-service/default | jq .
curl -s http://localhost:8888/planning-service/default | jq .
curl -s http://localhost:8888/task-service/default | jq .
curl -s http://localhost:8888/api-gateway/default | jq .
```

Eureka UI:

```bash
xdg-open http://localhost:8761
```

If you do not want to open a browser:

```bash
curl -s http://localhost:8761/eureka/apps | head
```

Expected apps after startup:

```text
AUTH-SERVICE
PROJECT-SERVICE
PLANNING-SERVICE
TASK-SERVICE
API-GATEWAY
```

Zipkin UI:

```bash
xdg-open http://localhost:9411
```

## 4. End-to-End API Test Through the Gateway

Set the gateway base URL:

```bash
BASE_URL=http://localhost:8080
EMAIL="tester-$(date +%s)@example.com"
PASSWORD="Password123!"
```

Register a user and save tokens:

```bash
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo "$REGISTER_RESPONSE" | jq .
ACCESS_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$REGISTER_RESPONSE" | jq -r '.refreshToken')
```

Optional login check:

```bash
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

echo "$LOGIN_RESPONSE" | jq .
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken')
```

Create a project:

```bash
PROJECT_RESPONSE=$(curl -s -X POST "$BASE_URL/projects" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jirops QA Project",
    "key": "QA",
    "description": "End-to-end test project"
  }')

echo "$PROJECT_RESPONSE" | jq .
PROJECT_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.id')
OWNER_ID=$(echo "$PROJECT_RESPONSE" | jq -r '.ownerId')
```

List projects:

```bash
curl -s "$BASE_URL/projects" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Create a sprint:

```bash
SPRINT_RESPONSE=$(curl -s -X POST "$BASE_URL/sprints" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"projectId\": \"$PROJECT_ID\",
    \"name\": \"Sprint 1\",
    \"goal\": \"Validate full app flow\",
    \"startDate\": \"2026-06-02\",
    \"endDate\": \"2026-06-16\"
  }")

echo "$SPRINT_RESPONSE" | jq .
SPRINT_ID=$(echo "$SPRINT_RESPONSE" | jq -r '.id')
```

List sprints for the project:

```bash
curl -s "$BASE_URL/sprints?projectId=$PROJECT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Start the sprint:

```bash
curl -s -X POST "$BASE_URL/sprints/$SPRINT_ID/start" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Create a task:

```bash
TASK_RESPONSE=$(curl -s -X POST "$BASE_URL/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"projectId\": \"$PROJECT_ID\",
    \"sprintId\": \"$SPRINT_ID\",
    \"title\": \"Verify E2E flow\",
    \"description\": \"Created through gateway during manual testing\",
    \"priority\": \"HIGH\",
    \"assignee\": \"$OWNER_ID\",
    \"labels\": [\"e2e\", \"qa\"]
  }")

echo "$TASK_RESPONSE" | jq .
TASK_ID=$(echo "$TASK_RESPONSE" | jq -r '.id')
```

Search tasks:

```bash
curl -s "$BASE_URL/tasks?projectId=$PROJECT_ID&status=TODO&priority=HIGH&assignee=$OWNER_ID&label=e2e" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Update the task:

```bash
curl -s -X PUT "$BASE_URL/tasks/$TASK_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Verify E2E flow - updated",
    "description": "Updated through gateway",
    "priority": "MEDIUM",
    "labels": ["e2e", "updated"]
  }' | jq .
```

Move the task status:

```bash
curl -s -X PATCH "$BASE_URL/tasks/$TASK_ID/status" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"IN_PROGRESS"}' | jq .
```

Add and list comments:

```bash
curl -s -X POST "$BASE_URL/tasks/$TASK_ID/comments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"body":"Manual E2E comment"}' | jq .

curl -s "$BASE_URL/tasks/$TASK_ID/comments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Add and list attachment metadata:

```bash
curl -s -X POST "$BASE_URL/tasks/$TASK_ID/attachments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "fileName": "test-log.txt",
    "contentType": "text/plain",
    "size": 128,
    "url": "https://example.test/files/test-log.txt"
  }' | jq .

curl -s "$BASE_URL/tasks/$TASK_ID/attachments" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Add the task to the sprint:

```bash
curl -s -X POST "$BASE_URL/sprints/$SPRINT_ID/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"taskId\":\"$TASK_ID\"}" | jq .
```

Complete the sprint. This should publish `sprint.completed`; Task Service consumes it and marks unfinished sprint tasks as `DONE`.

```bash
curl -s -X POST "$BASE_URL/sprints/$SPRINT_ID/complete" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .

sleep 3

curl -s "$BASE_URL/tasks/$TASK_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq .
```

Delete the project. This should publish `project.deleted`; Task Service consumes it and deletes project tasks.

```bash
curl -i -X DELETE "$BASE_URL/projects/$PROJECT_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"

sleep 3

curl -i "$BASE_URL/tasks/$TASK_ID" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Expected final task lookup result: `404 Not Found`.

Refresh token flow:

```bash
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

echo "$REFRESH_RESPONSE" | jq .
ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.refreshToken')
```

Logout:

```bash
curl -i -X POST "$BASE_URL/auth/logout" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

## 5. Direct Service Debug Commands

Use these only when the gateway is not running. Project and Planning require both a Bearer token and `X-User-Id` for controller methods that read that header. Task Service reads `X-User-Id`/`X-Roles` from the gateway header filter.

Decode the user ID from the JWT payload:

```bash
USER_ID=$(echo "$ACCESS_TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r '.sub')
echo "$USER_ID"
```

Direct Project Service create:

```bash
curl -s -X POST http://localhost:8082/projects \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -H "X-Roles: ROLE_USER" \
  -H "Content-Type: application/json" \
  -d '{"name":"Direct Project","key":"DIR","description":"Direct service test"}' | jq .
```

Direct Task Service search:

```bash
curl -s "http://localhost:8084/tasks?projectId=$PROJECT_ID" \
  -H "X-User-Id: $USER_ID" \
  -H "X-Roles: ROLE_USER" | jq .
```

## 6. Kafka Verification

List topics:

```bash
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Watch task status events:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic task.status_changed \
  --from-beginning
```

Watch sprint completed events:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic sprint.completed \
  --from-beginning
```

Watch project deleted events:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic project.deleted \
  --from-beginning
```

Check service logs for consumer activity:

```bash
docker compose logs task-service | tail -n 100
docker compose logs planning-service | tail -n 100
```

## 7. Database Verification

Auth users:

```bash
docker exec -it postgres-auth psql -U authuser -d authdb -c "select id, email, roles, created_at from users order by created_at desc limit 5;"
```

Projects:

```bash
docker exec -it postgres-project psql -U projectuser -d projectdb -c "select id, name, key, owner_id, created_at from projects order by created_at desc limit 5;"
```

Sprints:

```bash
docker exec -it postgres-planning psql -U planninguser -d planningdb -c "select id, project_id, name, status from sprints order by created_at desc limit 5;"
```

Mongo tasks:

```bash
docker exec -it mongodb mongosh taskdb --eval 'db.tasks.find({}, {title:1, projectId:1, sprintId:1, status:1, assignee:1}).sort({createdAt:-1}).limit(5).toArray()'
```

## 8. Negative Tests

No token should return `401` through the gateway:

```bash
curl -i "$BASE_URL/projects"
curl -i "$BASE_URL/tasks"
```

Invalid token should return `401`:

```bash
curl -i "$BASE_URL/projects" -H "Authorization: Bearer invalid"
```

Invalid project key should return validation error:

```bash
curl -i -X POST "$BASE_URL/projects" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Bad Project","key":"bad key","description":"Should fail"}'
```

Missing required task title should return validation error:

```bash
curl -i -X POST "$BASE_URL/tasks" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"$PROJECT_ID\",\"priority\":\"HIGH\"}"
```

## 9. Troubleshooting

If services do not start, inspect logs:

```bash
docker compose logs config-server
docker compose logs eureka-server
docker compose logs auth-service
docker compose logs project-service
docker compose logs planning-service
docker compose logs task-service
docker compose logs api-gateway
```

If the gateway cannot find services, verify Eureka registration:

```bash
curl -s http://localhost:8761/eureka/apps | grep -E "AUTH-SERVICE|PROJECT-SERVICE|PLANNING-SERVICE|TASK-SERVICE|API-GATEWAY"
```

If a service cannot reach Config Server, confirm the container is on the same network:

```bash
docker inspect config-server --format '{{json .NetworkSettings.Networks}}' | jq .
docker inspect api-gateway --format '{{json .NetworkSettings.Networks}}' | jq .
```

If a port is already used:

```bash
sudo lsof -i :8080
sudo lsof -i :8081
sudo lsof -i :8082
sudo lsof -i :8083
sudo lsof -i :8084
```

If you need a clean state:

```bash
docker compose down -v
docker compose up --build -d
```
