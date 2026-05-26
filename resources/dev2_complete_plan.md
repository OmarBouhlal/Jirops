# Coding Agent Execution Plan: Dev 2 (Business Services & Shared Lib)

## Role & Mission
You are acting as **Dev 2** in a two-developer team building a microservices-based Jira clone. 
Your primary responsibilities include building the shared JWT security library, scaffolding the business microservices, and implementing the complete Task Service (using MongoDB).

## Tech Stack (Dev 2 Focus)
- Java & Spring Boot (3.2.4)
- Maven (Mono-repo structure)
- MongoDB (Task Service database)
- PostgreSQL (Project/Planning Service connections)
- Spring Cloud (Eureka Client, OpenFeign)
- Apache Kafka (Event-driven architecture)
- Micrometer/Zipkin (Distributed tracing)

---

## Phase 1: Service Skeletons (No Logic Yet)
*Dependency: Dev 1 must have the `docker-compose` running with DBs and Eureka.*
**Branch:** `feature/dev2/service-skeletons`

1. **Create Project Service Skeleton**
   - Spring Boot app.
   - Connect to PostgreSQL.
   - Register as Eureka client.
   - Add Kafka producer dependency, Zipkin/Sleuth tracing, Health check endpoints.
2. **Create Task Service Skeleton**
   - Spring Boot app.
   - Connect to MongoDB.
   - Register as Eureka client.
   - Add Kafka, Zipkin, Health check endpoints.
3. **Create Planning Service Skeleton**
   - Spring Boot app.
   - Connect to PostgreSQL.
   - Register as Eureka client.
   - Add Kafka, Zipkin, Health check endpoints.

## Phase 2: Auth & Shared Library
**Branch 1:** `feature/dev2/jwt-commons` (High Priority - Blocked Dev 1)
1. **Create `jwt-commons` Library module** (Standard JAR, NOT a Spring Boot app).
2. Create `JwtTokenProvider` to sign/verify tokens and extract claims (UserId, Roles).
3. Create `JwtAuthenticationFilter` (Spring Security filter) to extract User ID/Roles into `SecurityContext`.
4. Write unit tests for token edge cases (valid, expired, tampered).

**Branch 2:** `feature/dev2/secure-services`
1. Import `jwt-commons` into Project, Task, and Planning services.
2. Enable Spring Security in each service.
3. Read `X-User-Id` / `X-Roles` headers forwarded by the API Gateway for resource ownership.
4. Add `@PreAuthorize` role checks to controllers.

## Phase 3: Business Logic (Task Service)
**Branch:** `feature/dev2/task-service`
1. **Task Document Entity (MongoDB):** `id`, `projectId`, `title`, `description`, `status`, `priority`, `assignee`, `reporter`, `labels`, `createdAt`.
2. **REST API (Full CRUD):**
   - `POST /tasks` - Create task
   - `GET /tasks?projectId=` - List by project
   - `GET /tasks/{id}` - Get single task
   - `PUT /tasks/{id}` - Update fields
   - `PATCH /tasks/{id}/status` - Move on board
   - `DELETE /tasks/{id}` - Delete task
3. **Extra Features Sub-resources:**
   - Comments: `POST/GET /tasks/{id}/comments`
   - Attachments metadata: `POST/GET /tasks/{id}/attachments`
   - Task Search/Filter logic (status, priority, assignee, label).
4. **Kafka Integration (Event Driven):**
   - Publish: `task.created`, `task.status_changed`.
   - Consume: `project.deleted` (Cascade delete tasks).
   - Consume: `sprint.completed` (Mark unfinished tasks).

## Phase 4: Testing & Polish
1. Unit tests for Task Service CRUD, comments, and search.
2. Integration tests for Kafka consumers.
3. Update OpenAPI/Swagger documentation or Postman collection with Bearer Auth setup.
4. Verify Eureka and Zipkin traces for all Dev 2 services.

---
## Standard Workflow for Agent
1. **Never commit directly to `main` or `develop`.**
2. Always branch from `develop`: `git checkout -b feature/dev2/<task-name>`
3. Follow commit convention: `feat(scope): desc`, `fix(scope): desc`, `chore(scope): desc`.
