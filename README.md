# TicketFlow

A ticket and incident tracker modelling real support workflows: priorities, status transitions, comments, and a full audit trail. Built as a portfolio project and developed in public.

## What's implemented

- **Ticket CRUD** — create, list (with filters), get by ID, patch
- **Status workflow** — enforced transitions: `OPEN → IN_PROGRESS → WAITING → RESOLVED → CLOSED`
- **Priority levels** — P1 (critical) through P4 (low)
- **Assignee** — optional, blank/null treated as unassigned
- **Comments** — per-ticket comment thread
- **Audit events** — `ticket_events` table captures every meaningful change (status, priority, assignee, title, description, comment added)
- **Swagger/OpenAPI** — auto-generated at `/swagger-ui/index.html`
- **CORS** — configured for `http://localhost:5173` (frontend dev server)
- **Frontend UI** — React + TypeScript + Vite SPA at `frontend/` with Vite dev proxy
  (frontend calls `/api/...`, Vite forwards to the backend) so local dev does not depend on CORS
- **Inline ticket editing** — title, description, priority, and assignee can be edited from the detail panel
- **Manual refresh** — top-bar Refresh button re-loads health, list, and selected ticket
- **Open-Session-In-View disabled** — JPA sessions do not leak into the web layer

## Tech stack

| Layer      | Technology                            |
|------------|---------------------------------------|
| Backend    | Java 17, Spring Boot, Spring Data JPA |
| Database   | PostgreSQL 16 (Docker Compose, port 5433) |
| Migrations | Flyway                                |
| Tests      | JUnit 5, Spring Boot Test, H2 (test)  |
| API docs   | Springdoc OpenAPI / Swagger UI        |
| Frontend   | React 18, TypeScript, Vite            |

## Running the full stack locally

**Prerequisites:** Java 17, Docker Desktop, Node.js 18+

**Terminal 1 — database:**
```bash
docker compose up -d
```

**Terminal 2 — backend:**
```powershell
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

**Terminal 3 — frontend:**
```bash
cd frontend
npm install
npm run dev
```

**URLs:**

| Service  | URL                                         |
|----------|---------------------------------------------|
| Backend  | http://localhost:8080                       |
| Swagger  | http://localhost:8080/swagger-ui/index.html |
| Frontend | http://localhost:5173                       |

## Configuration

All configuration is optional — defaults work for the standard Docker Compose setup.

| Variable                     | Default                                       |
|------------------------------|-----------------------------------------------|
| `SPRING_DATASOURCE_URL`      | `jdbc:postgresql://localhost:5433/ticketflow`  |
| `SPRING_DATASOURCE_USERNAME` | `postgres`                                    |
| `SPRING_DATASOURCE_PASSWORD` | `postgres`                                    |
| `SERVER_PORT`                | `8080`                                        |
| `FRONTEND_ORIGIN`            | `http://localhost:5173`                       |
| `VITE_API_BASE_URL`          | `/api` (relative; routed through the Vite dev proxy) |
| `VITE_BACKEND_TARGET`        | `http://localhost:8080` (proxy target during dev)    |

Copy `.env.example` to `.env` for backend variables. Copy `frontend/.env.example` to `frontend/.env` for frontend variables.

> **Note:** Docker Compose maps the container's port 5432 to **host port 5433** to avoid conflicts with a locally installed PostgreSQL.

### Running the backend on a non-default port

If port 8080 is occupied, start the backend on a different port and update the Vite proxy target:

```powershell
# Terminal 2 – backend on 8081
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run
```

```bash
# frontend/.env
VITE_BACKEND_TARGET=http://localhost:8081
```

Then restart `npm run dev` so Vite picks up the new env value.

## API overview

```
GET  /
GET  /health
POST /tickets
GET  /tickets?status=&priority=&q=
GET  /tickets/{id}
PATCH /tickets/{id}
POST /tickets/{id}/comments
GET  /tickets/{id}/comments
GET  /tickets/{id}/events
```

## Status workflow

```
OPEN → IN_PROGRESS
IN_PROGRESS → WAITING, RESOLVED
WAITING → IN_PROGRESS, RESOLVED
RESOLVED → CLOSED
CLOSED → (terminal)
```

## Running tests

```powershell
# Windows
.\mvnw.cmd clean test

# macOS / Linux
./mvnw clean test
```

Tests use an in-memory H2 database — no running Docker container required.

## Troubleshooting

See [docs/RUN_AND_TEST_GUIDE.md](docs/RUN_AND_TEST_GUIDE.md) for the full troubleshooting guide including frontend/backend connectivity issues.

## Project structure

```
src/main/java/.../
  api/           # Root/health controllers, CORS config, error handling
  tickets/       # Ticket domain: entity, service, controller, repositories, DTOs
src/main/resources/
  application.yml
  db/migration/  # Flyway SQL migrations
frontend/
  src/
    api/         # Typed API client and TypeScript types
    components/  # React components
  .env.example
```

## Roadmap

- [ ] User identity / actor names
- [ ] Pagination on ticket list
- [ ] Keyboard shortcuts
- [ ] Comment editing / deletion

## License

MIT