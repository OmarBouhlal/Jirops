# Jira Clone: Master Project Architecture & Execution Plan

## Architecture Overview
A microservices-based Jira clone built from scratch using Java, Spring Boot, and Maven in a Mono-repo structure. 

### Infrastructure Core (Owned by Dev 1)
- **API Gateway:** Spring Cloud Gateway (Entrypoint, JWT enforcement).
- **Service Discovery:** Netflix Eureka.
- **Configuration:** Spring Cloud Config Server (Single source of secrets).
- **Message Broker:** Apache Kafka (Event-driven communication).
- **Tracing:** Zipkin / Micrometer.
- **Databases:** PostgreSQL (x2), MongoDB.

### Microservices
- **Auth Service (Dev 1):** JWT generation, user registration/login. PostgreSQL.
- **Project Service (Dev 1):** Project CRUD, team members. PostgreSQL.
- **Planning Service (Dev 1):** Sprints lifecycle. PostgreSQL.
- **Task Service (Dev 2):** Task management, boards, comments, filters. MongoDB.

---

## Phase 1 — Skeleton & Infrastructure
**Goal:** Stand up the plumbing. No business logic.
- **Dev 1:** Setup Mono-repo `pom.xml`, `.gitignore`. Create the massive `docker-compose.yml` containing PostgreSQL x2, MongoDB, Kafka, Zookeeper, Zipkin, and Eureka. Spin up Config Server, Eureka Server, and API Gateway (no auth yet).
- **Dev 2:** Create the 3 Spring Boot skeletons (Project, Task, Planning). Configure them to connect to their respective DBs, register with Eureka, and include Kafka/Zipkin dependencies.

## Phase 2 — Auth & Shared Library
**Goal:** Secure the perimeter and internal service-to-service calls.
- **Dev 2 (BLOCKER):** Create `jwt-commons` (Shared Library). Contains `JwtTokenProvider` and `JwtAuthenticationFilter`. Must be finished first so Dev 1 can import it.
- **Dev 1:** Build Auth Service. Implement `/register`, `/login`, `/refresh`. Generate JWTs. Add Global JWT Filter to API Gateway to enforce auth and forward `X-User-Id` headers downstream.
- **Dev 2:** Import `jwt-commons` into the 3 business services. Secure endpoints using `@PreAuthorize` and extract headers. Test using a hardcoded stub token until Gateway is ready.

## Phase 3 — Business Logic (Parallel Work)
**Goal:** Implement the core Jira features. 
- **Dev 1:** Implement Project Service (Full CRUD, publish `project.created`/`deleted` to Kafka). Implement Planning Service (Sprint CRUD, consume `task.status_changed`, publish `sprint.started`/`completed`).
- **Dev 2:** Implement Task Service. Full CRUD using MongoDB. Add Sub-resources for Comments and Attachments. Build complex search/filtering. Publish `task.status_changed`. Consume `project.deleted` to cascade delete tasks.

## Phase 4 — Testing, Polish, & E2E Integration
- **Dev 1:** Unit test Auth, Project, Planning. Integration test Gateway routing. Verify Eureka and Zipkin traces.
- **Dev 2:** Unit test `jwt-commons`, Task Service. Integration test Kafka consumers. Build OpenAPI docs.
- **Together:** Full end-to-end flow. Register -> Login -> Create Project -> Create Sprint -> Create Task -> Move Task -> Complete Sprint.

---

## Git Strategy & Execution Order
1. **Branches:** `main` (Protected, production-ready), `develop` (Integration branch, test together here).
2. **Feature Branches:** `feature/devX/<feature-name>`.
3. **Merge Flow:** PRs required. Feature -> `develop` -> `main`. 
4. **Execution Chokepoints:**
   - Dev 1 must push `docker-compose.yml` first so Dev 2 can test locally.
   - Dev 2 must push `jwt-commons` first so Dev 1 can build the Gateway security filter.
