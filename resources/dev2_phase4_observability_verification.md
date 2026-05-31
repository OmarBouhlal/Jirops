# Dev 2 Phase 4 Observability Verification

Use this checklist after starting the stack with Docker Compose.

## Services Covered
- `project-service`
- `task-service`
- `planning-service`

## Configuration Checks
- All three services have Eureka client configuration pointing at `http://eureka-server:8761/eureka/`.
- All three services configure Zipkin export to `http://zipkin:9411/api/v2/spans`.
- All three services set `management.tracing.sampling.probability: 1.0` for local verification.
- All three services expose actuator `health` and `info` endpoints.

## Runtime Verification
1. Start infrastructure and services:
   ```bash
   docker compose up --build
   ```
2. Open Eureka at `http://localhost:8761`.
3. Confirm the registry contains:
   - `PROJECT-SERVICE`
   - `TASK-SERVICE`
   - `PLANNING-SERVICE`
4. Exercise Dev 2 traffic through the gateway using `resources/Jirops_Dev2_Task_Service.postman_collection.json`:
   - Login or register to set `bearerToken`.
   - Create a task.
   - Search tasks.
   - Move task status.
   - Add and list comments.
5. Open Zipkin at `http://localhost:9411`.
6. Search traces for `api-gateway`, `project-service`, `task-service`, and `planning-service`.
7. Confirm Task Service spans appear for:
   - `POST /tasks`
   - `GET /tasks`
   - `PATCH /tasks/{id}/status`
   - Kafka publish of `task.created` and `task.status_changed` during task creation/status movement.
8. Confirm Planning Service receives task status-change traffic by checking service logs for:
   ```text
   Task status changed: taskId=...
   ```

## Notes
- Direct calls to `task-service` need gateway-forwarded identity headers. The Postman collection targets the gateway at `http://localhost:8080` and uses Bearer auth.
- If a service is missing in Eureka, check that the Config Server is healthy and the service can resolve `eureka-server` on the `jira-net` Docker network.
- If traces are missing, verify Zipkin is reachable from the service container and that traffic was generated after the service started.
