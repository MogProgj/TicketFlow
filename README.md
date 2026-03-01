# TicketFlow

> **Status:** Public build started on 2026-03-01. This repo is being developed in public from MVP onward.

TicketFlow is a small ticket and incident tracker designed to model real support workflows: priorities, status transitions, comments, and an audit trail of changes. It’s meant to be simple enough to run locally, but structured enough to look like something you’d actually maintain on a team.

This repo is being built in public as a portfolio project.

## What it will support (MVP)
Tickets with:
Title and description
Priority (P1–P4)
Assignee
Status workflow and timestamps
Comments
Event history (who changed what, and when)

## Status workflow
The MVP will enforce a basic workflow to avoid “anything goes” updates:
OPEN → IN_PROGRESS → WAITING → RESOLVED → CLOSED

## Tech stack
Java, Spring Boot, PostgreSQL, Docker, Flyway, JUnit, Swagger/OpenAPI.

## Quickstart (local)
Prerequisites:
Java 17+ and Docker.

1) Start the database
docker compose up -d

2) Run the app
./mvnw spring-boot:run

3) Open API docs
http://localhost:8080/swagger-ui/index.html

## Configuration
Example environment values:

SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ticketflow
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

## API overview (planned)
Tickets
POST /tickets
GET /tickets?status=&priority=&q=
GET /tickets/{id}
PATCH /tickets/{id}

Comments
POST /tickets/{id}/comments

History
GET /tickets/{id}/events

## Database model (planned)
tickets
ticket_comments
ticket_events

The ticket_events table is the “grown-up” feature here: it keeps an audit trail for status changes and important updates, which is how real support platforms avoid chaos.

## Project structure (planned)
src/main/java/.../
controller/
service/
repository/
model/
src/main/resources/
application.yml
db/migration/

## Roadmap
- [ ] Schema + migrations
- [ ] Ticket CRUD
- [ ] Status transition validation
- [ ] Comments
- [ ] Audit trail (ticket_events)
- [ ] Search and filters
- [ ] Seed data for demo
- [ ] Basic auth + roles (optional)

## License
MIT
