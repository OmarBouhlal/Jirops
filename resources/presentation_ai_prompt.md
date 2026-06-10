# Prompt for AI Presentation Generator

You are an expert technical presentation writer.

Create a detailed presentation that explains the microservice architecture of this project in depth.

The presentation should be clear enough for a technical audience, but also structured so that a non-expert can follow the overall system design.

## Goal

Explain how the platform is built, how the services communicate, how security works, where data is stored, and why the architecture is split the way it is.

## Context

This project is a Jira-like application implemented as a Java Spring Boot microservices platform in a monorepo.

The main components are:

- `api-gateway`
- `auth-service`
- `project-service`
- `planning-service`
- `task-service`
- `config-server`
- `eureka-server`
- `jwt-commons`
- `frontend`

Infrastructure used by the platform:

- PostgreSQL for `auth-service`
- PostgreSQL for `project-service`
- PostgreSQL for `planning-service`
- MongoDB for `task-service`
- Kafka for asynchronous events
- Eureka for service discovery
- Spring Cloud Config Server for centralized configuration
- Zipkin for distributed tracing

The services are wired through Docker Compose and communicate through the gateway and Kafka.

## Architecture Facts You Must Reflect

Use these facts as the source of truth. Do not invent unsupported components or flows.

### Entry Point and Routing

- The `api-gateway` is the public entry point.
- It listens on port `8080`.
- It routes requests by path:
  - `/auth/**` -> `auth-service`
  - `/projects/**` -> `project-service`
  - `/sprints/**` -> `planning-service`
  - `/tasks/**` -> `task-service`
- The gateway validates JWT access tokens before forwarding requests.
- The gateway extracts identity from the token and forwards:
  - `X-User-Id`
  - `X-Roles`

### Authentication and Authorization

- `auth-service` exposes:
  - `POST /auth/register`
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
- `auth-service` uses PostgreSQL.
- It issues access and refresh tokens.
- `jwt-commons` provides shared JWT/security support used by the gateway and downstream services.
- Business services rely on forwarded headers plus Spring Security method security such as `@PreAuthorize`.
- The downstream services have security test endpoints that demonstrate the forwarded user identity being converted into an authenticated principal.

### Project Service

- Runs on port `8082`.
- Uses PostgreSQL.
- Handles project CRUD and member management.
- Receives the authenticated user ID through `X-User-Id`.
- Publishes Kafka events:
  - `project.created`
  - `project.deleted`

### Planning Service

- Runs on port `8083`.
- Uses PostgreSQL.
- Manages sprint lifecycle operations.
- Publishes Kafka events:
  - `sprint.started`
  - `sprint.completed`
- Consumes the task lifecycle event:
  - `task.status_changed`

### Task Service

- Runs on port `8084`.
- Uses MongoDB.
- Handles task CRUD, comments, attachments, search, filters, and status updates.
- Publishes Kafka events:
  - `task.created`
  - `task.status_changed`
- Consumes:
  - `project.deleted`
  - `sprint.completed`

### Configuration and Discovery

- `config-server` centralizes configuration for the services.
- `eureka-server` provides service discovery.
- Services register with Eureka and resolve each other by logical service name.
- The platform is containerized with Docker Compose.

### Observability

- Zipkin is used for distributed tracing.
- Services and event publishers add trace-related logging and span tags.
- The architecture should be presented as observable and production-oriented, not just a toy demo.

## Recommended Presentation Structure

Generate a presentation of about 12 to 15 slides.

### Slide 1: Title

- Project name
- Short subtitle describing it as a Jira-like microservices platform
- One-sentence value proposition

### Slide 2: Problem and Motivation

- Why split a Jira-style platform into microservices
- What problems this architecture solves
- Trade-offs versus a monolith

### Slide 3: High-Level Architecture Overview

- Show the complete system at a glance
- Include frontend, gateway, auth, project, planning, task, config server, Eureka, Kafka, databases, and Zipkin
- Explain the role of each box in one or two sentences

### Slide 4: Runtime and Infrastructure

- Explain Docker Compose
- Explain the database-per-service approach
- Explain how Kafka, Zookeeper, Eureka, Config Server, and Zipkin fit into the platform

### Slide 5: Request Flow Through the Gateway

- Show how a user request enters through the gateway
- Show JWT validation
- Show header forwarding with `X-User-Id` and `X-Roles`
- Explain why downstream services do not directly trust client requests

### Slide 6: Authentication Flow

- Registration, login, token issuance, refresh, logout
- Explain access token versus refresh token
- Explain why auth is isolated in its own service

### Slide 7: Shared Security Model

- Explain `jwt-commons`
- Explain how the gateway and services share a common identity model
- Explain method-level authorization with Spring Security

### Slide 8: Project Service Deep Dive

- Responsibilities
- Data ownership
- Main API actions
- Events emitted to Kafka

### Slide 9: Planning Service Deep Dive

- Responsibilities
- Sprint lifecycle
- How it reacts to task status changes
- Events emitted to Kafka

### Slide 10: Task Service Deep Dive

- Responsibilities
- MongoDB as the document store
- Comments and attachments as sub-resources
- Search/filtering
- Events emitted and consumed

### Slide 11: Event-Driven Communication

- Show the event flow between services
- Include these examples:
  - project deleted -> task cleanup
  - sprint completed -> tasks updated
  - task status changed -> planning reacts
- Explain why Kafka was chosen for asynchronous coordination

### Slide 12: Data Ownership and Persistence Strategy

- Explain why each service owns its own database
- Contrast PostgreSQL and MongoDB in this system
- Explain migration and schema validation at a high level

### Slide 13: Observability and Operations

- Explain Zipkin tracing
- Explain how trace IDs help follow a request across services
- Mention health endpoints and operational visibility

### Slide 14: End-to-End User Journey

- Walk through a realistic flow:
  - user registers
  - user logs in
  - user creates a project
  - user creates tasks
  - user updates task status
  - sprint completes
  - related services react through Kafka

### Slide 15: Benefits and Trade-Offs

- Benefits:
  - independent scaling
  - clearer ownership
  - better fault isolation
  - polyglot persistence
  - event-driven decoupling
- Trade-offs:
  - more operational complexity
  - distributed debugging
  - eventual consistency
  - more infrastructure components

### Slide 16: Conclusion

- Summarize the architecture in one clear paragraph
- End with the key architectural lesson from the project

## Visual Requirements

- Include at least one system architecture diagram.
- Include at least one request/sequence diagram for the auth flow.
- Include at least one event flow diagram for Kafka communication.
- Include icons or visual cues for:
  - gateway
  - auth/security
  - databases
  - Kafka events
  - observability
- Keep the layout professional, modern, and easy to scan.

## Style Requirements

- Use clear technical English.
- Be precise and factual.
- Prefer simple language over jargon when possible.
- Explain acronyms the first time they appear.
- Keep the narrative coherent from infrastructure -> security -> services -> events -> observability.
- Use concrete examples instead of vague descriptions.

## Important Constraints

- Do not claim technologies that are not present in the architecture.
- Do not add external services or features unless they are explicitly implied by the codebase.
- Do not present the frontend as the main focus; it is only the client of the microservices platform.
- Do not omit the role of the gateway, Config Server, Eureka, Kafka, or Zipkin.
- Do not describe the architecture as serverless, event-only, or monolithic.

## Suggested Output Format For The Presentation

Write the presentation content slide by slide.

For each slide provide:

- Slide title
- 3 to 6 bullet points
- Speaker notes or explanation paragraphs
- Suggested diagram content if relevant

If possible, also include:

- a short executive summary
- a one-paragraph architecture overview
- a closing summary that can be used as the final slide

## Final Deliverable

Generate a presentation that explains this architecture in detail, with enough depth that a technical reviewer could understand:

- the purpose of every service
- how requests travel through the platform
- how identity and authorization are enforced
- how the services coordinate with Kafka
- how the system is observed and operated

