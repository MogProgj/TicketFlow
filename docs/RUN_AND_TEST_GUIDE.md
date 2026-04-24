# Run And Test Guide

## Prerequisites

- Java 17
- Maven or the project wrapper (`mvnw` / `mvnw.cmd`)
- Docker Desktop
- PostgreSQL via Docker Compose
- Optional IDE: IntelliJ IDEA or VS Code

## Environment setup

The application is configured for a local PostgreSQL database by default.

- `SPRING_DATASOURCE_URL` defaults to `jdbc:postgresql://localhost:5433/ticketflow`
- `SPRING_DATASOURCE_USERNAME` defaults to `postgres`
- `SPRING_DATASOURCE_PASSWORD` defaults to `postgres`
- `SERVER_PORT` defaults to `8080`

You only need to set those environment variables if your local values differ from the defaults.

Docker Compose starts the local PostgreSQL instance defined in [docker-compose.yml](../docker-compose.yml). It maps the container's internal port 5432 to **host port 5433** to avoid conflicts with any native PostgreSQL installation. The app connects to that database and Flyway applies the SQL migrations on startup.

## Start the database

```bash
docker compose up -d
```

Stop it with:

```bash
docker compose down
```

## Run the backend locally

macOS / Linux / Git Bash:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The app starts on port `8080` by default.

## Confirm the app is running

API info (root):

```http
GET http://localhost:8080/
```

Expected response:

```json
{
  "service": "TicketFlow",
  "status": "ok",
  "health": "/health",
  "tickets": "/tickets",
  "swagger": "/swagger-ui/index.html"
}
```

Health check:

```http
GET http://localhost:8080/health
```

Expected response:

```json
{ "status": "ok" }
```

## Run automated tests

macOS / Linux:

```bash
./mvnw test
```

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

The test suite uses H2 with Flyway disabled for fast local feedback.

## Manually test the API

You can use [requests/api.http](../requests/api.http), Swagger UI, Postman, or curl.

Create a ticket:

```http
POST http://localhost:8080/tickets
Content-Type: application/json

{
  "title": "Login page returns 500",
  "description": "Users see an internal server error on the login page.",
  "priority": "P1",
  "assignee": "alice"
}
```

Expected behavior: returns `201 Created` and the new ticket with status `OPEN`.

List tickets:

```http
GET http://localhost:8080/tickets
GET http://localhost:8080/tickets?status=OPEN
GET http://localhost:8080/tickets?priority=P1
GET http://localhost:8080/tickets?q=login
```

Get one ticket:

```http
GET http://localhost:8080/tickets/1
```

Partially update a ticket:

```http
PATCH http://localhost:8080/tickets/1
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

Fetch ticket events:

```http
GET http://localhost:8080/tickets/1/events
```

Expected behavior: events are returned in ascending creation order. A new ticket records a `CREATED` event, and a status change records a `STATUS_CHANGED` event.

## Status workflow testing

Valid transitions:

- `OPEN -> IN_PROGRESS`
- `IN_PROGRESS -> WAITING`
- `IN_PROGRESS -> RESOLVED`
- `WAITING -> IN_PROGRESS`
- `WAITING -> RESOLVED`
- `RESOLVED -> CLOSED`

Valid example:

```http
PATCH http://localhost:8080/tickets/1
Content-Type: application/json

{
  "status": "IN_PROGRESS"
}
```

Invalid example:

```http
PATCH http://localhost:8080/tickets/1
Content-Type: application/json

{
  "status": "CLOSED"
}
```

Expected behavior: the invalid request returns `400 Bad Request` with a readable message explaining the rejected transition.

## Swagger and OpenAPI

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Troubleshooting

### Port 8080 already in use

If the app fails to start with "Port 8080 was already in use", find what is holding the port:

```powershell
netstat -ano | findstr :8080
```

Identify the process name from the PID shown:

```powershell
tasklist /FI "PID eq <PID>"
```

Stop it if safe:

```powershell
taskkill /PID <PID> /F
```

Alternatively, start the app on a different port without stopping the other process.

Windows PowerShell:

```powershell
$env:SERVER_PORT=8081
.\mvnw.cmd spring-boot:run
```

macOS / Linux / Git Bash:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

Maven argument alternative (Windows PowerShell):

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

When running on port 8081, the URLs become:

```text
http://localhost:8081/
http://localhost:8081/health
http://localhost:8081/swagger-ui/index.html
```

Note: `docker compose down` only stops the Postgres container. If a Spring Boot process is still running on port 8080 from a previous session, you must stop it separately using the steps above.

### Whitelabel Error Page (404)

If you see Spring Boot's Whitelabel Error Page, you opened a route that does not exist.

- `GET /` returns API info and is the correct starting point.
- `GET /health` returns `{ "status": "ok" }`.
- `GET /swagger-ui/index.html` opens the interactive API docs.

Common mistakes: visiting `/api`, `/index`, `/home`, or a ticket ID that does not exist.

### Docker not running

Start Docker Desktop first, then rerun:

```bash
docker compose up -d
```

### Database connection failure

Verify PostgreSQL is running on `localhost:5433` (the host port mapped in `docker-compose.yml`) and that your datasource environment variables match the container credentials (`postgres` / `postgres` by default).

### Migrations not applied

Check the app startup logs for Flyway errors. If the local database has stale or inconsistent state, recreate the volume:

```bash
docker compose down -v
docker compose up -d
```

### Tests pass but local Postgres fails

Tests use H2 in-memory with `spring.flyway.enabled=false`. Passing tests do not guarantee the app works against local Postgres. Always start the Docker container and run the app before treating a feature as verified.

### Windows command differences

Use `.\mvnw.cmd ...` in PowerShell instead of `./mvnw ...`.
