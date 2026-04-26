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
- `FRONTEND_ORIGIN` defaults to `http://localhost:5173` (CORS allowed origin)

Copy `.env.example` to `.env` and edit the values if your setup differs. All five variables are optional — the defaults work out of the box for the standard Docker Compose setup.

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

Expected behavior: events are returned in ascending creation order. A new ticket records a `CREATED` event. Field changes (title, description, priority, assignee, status) each emit a separate typed event (`TITLE_CHANGED`, `PRIORITY_CHANGED`, `ASSIGNEE_CHANGED`, `DESCRIPTION_CHANGED`, `STATUS_CHANGED`) only when the value actually changes. No event is emitted if the new value equals the old value.

Create and list comments:

```http
POST http://localhost:8080/tickets/1/comments
Content-Type: application/json

{
  "author": "alice",
  "body": "Reproduced on staging."
}

GET http://localhost:8080/tickets/1/comments
```

Expected behavior: `POST` returns `201 Created` with the comment fields; `GET` returns comments in ascending creation order. Posting a comment also emits a `COMMENT_ADDED` event (actor = comment author).

Assignee behavior:
- Omit `assignee` or send `null` on create → stored as `null`.
- Send a blank string (`""` or `"   "`) on `PATCH` → clears assignee to `null` and emits `ASSIGNEE_CHANGED`.

Unknown JSON fields are rejected with `400 Bad Request`. The error response identifies the unknown field name.

## CORS

The API allows cross-origin requests from `FRONTEND_ORIGIN` (default `http://localhost:5173`). Allowed methods: `GET`, `POST`, `PATCH`, `DELETE`, `OPTIONS`. Allowed headers: `Content-Type`, `Authorization`.

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

Check the app startup logs for Flyway errors. The schema is managed by Flyway migrations in `src/main/resources/db/migration/`. Do not edit `V1__init.sql`; add new migrations as `V2__...`, `V3__...`, etc. If the local database has stale or inconsistent state, recreate the volume:

```bash
docker compose down -v
docker compose up -d
```

### Tests pass but local Postgres fails

Tests use H2 in-memory with `spring.flyway.enabled=false`. Passing tests do not guarantee the app works against local Postgres. Always start the Docker container and run the app before treating a feature as verified.

### Windows command differences

Use `.\mvnw.cmd ...` in PowerShell instead of `./mvnw ...`. The `mvnw` shell script uses LF line endings and runs on Linux, macOS, and Git Bash/CI. The `mvnw.cmd` wrapper is for Windows CMD and PowerShell.

### UnsupportedClassVersionError (class file version mismatch)

**Symptom:**

```
UnsupportedClassVersionError: com/mogproj/ticketflow/TicketFlowApplication
  has been compiled by a more recent version of the Java Runtime
  (class file version 69.0), but this runtime only recognizes class file
  versions up to 61.0
```

**Explanation:** Class file version `69.0` means the app was compiled for Java 25. Version `61.0` is Java 17. The compiled classes in `target/` do not match the runtime JDK.

**Root cause:** Spring Boot 3.5.x parent POM sets `maven.compiler.release` to match the running JDK version independently of the project's `<java.version>` property. Setting only `<java.version>17</java.version>` is insufficient — `maven.compiler.release` must also be pinned explicitly.

**Fix:**

1. Confirm `pom.xml` `<properties>` contains **both**:
   ```xml
   <java.version>17</java.version>
   <maven.compiler.release>17</maven.compiler.release>
   ```
2. Wipe compiled output and rebuild from source:

   Windows PowerShell:

   ```powershell
   .\mvnw.cmd clean
   .\mvnw.cmd test
   .\mvnw.cmd spring-boot:run
   ```

   macOS / Linux:

   ```bash
   ./mvnw clean
   ./mvnw test
   ./mvnw spring-boot:run
   ```

3. To run on an alternate port:

   ```powershell
   $env:SERVER_PORT="8081"
   .\mvnw.cmd spring-boot:run
   ```

### Committed build outputs

The `target/` directory is in `.gitignore` and should never be committed. If it appears tracked, run `git rm -r --cached target/` and commit the result.

---

## Running the frontend

The frontend is a Vite + React + TypeScript app in `frontend/`.

### Prerequisites

Node.js 18 or later. Verify with:

```powershell
node -v
npm -v
```

### First-time setup

```bash
cd frontend
npm install
```

### Development server

```bash
npm run dev
```

Vite starts on port 5173 by default: http://localhost:5173

The backend must be running before the frontend can load tickets.

### Production build

```bash
npm run build
```

Output goes to `frontend/dist/`. Serve it with `npm run preview` to verify locally.

### Type-check

```bash
npm run typecheck
```

### Frontend environment variables

Copy `frontend/.env.example` to `frontend/.env` and set:

```
VITE_API_BASE_URL=http://localhost:8080
```

The default is `http://localhost:8080` if the variable is absent.

---

## Frontend troubleshooting

### Frontend cannot reach backend (CORS error or network error)

1. Confirm the backend is running: `GET http://localhost:8080/health` should return `{ "status": "ok" }`.
2. Confirm `VITE_API_BASE_URL` in `frontend/.env` points to the correct backend URL and port.
3. Confirm the backend's `FRONTEND_ORIGIN` variable matches the frontend origin exactly (including port). Default is `http://localhost:5173`.

### Backend running on a non-default port

If the backend is on port 8081:

```
VITE_API_BASE_URL=http://localhost:8081
```

Also ensure the backend is started with the matching `FRONTEND_ORIGIN`:

```powershell
$env:FRONTEND_ORIGIN="http://localhost:5173"
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run
```

### Vite choosing a different port

If port 5173 is already in use, Vite picks the next available port (e.g., 5174). Update `FRONTEND_ORIGIN` accordingly before starting the backend:

```powershell
$env:FRONTEND_ORIGIN="http://localhost:5174"
.\mvnw.cmd spring-boot:run
```

### Health indicator shows "Backend offline"

- Check the backend is running and reachable at the configured `VITE_API_BASE_URL`.
- Check the browser console for network errors — a CORS error means `FRONTEND_ORIGIN` does not match the frontend's actual origin.
- The health check polls every 30 seconds; the indicator updates automatically when the backend comes online.
