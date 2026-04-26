# TicketFlow

A ticket and incident tracker modelling real support workflows: priorities, status transitions, comments, and a full audit trail. Built as a portfolio project and developed in public.

## Quick start (read this first)

If `npm install` complains about a missing `package.json`, or the backend log says
`spring-boot:3.2.5:run` / `Found 2 JPA repository interfaces`, your local checkout
is on an old commit that does not have the full app. Pull the latest first:

```powershell
cd C:\PyOps\TicketFlow
git fetch origin
git checkout claude/inspect-ticketflow-bVddW
git pull
```

The latest code targets **Spring Boot 3.5.14** and has **3 JPA repositories**
(Ticket, TicketComment, TicketEvent). If you do not see those numbers, you are
not on the latest commit.

### Prerequisites

- **Java 17** (e.g. Temurin 17). Verify: `java -version`
- **Docker Desktop** for the local Postgres container
- **Node.js 18+** for the frontend. Verify: `node -v`

### One-time setup

```powershell
# 1. Start Postgres (host port 5433, container port 5432)
cd C:\PyOps\TicketFlow
docker compose up -d

# 2. Install frontend dependencies
cd C:\PyOps\TicketFlow\frontend
npm install

# 3. (Optional) copy env templates
cd C:\PyOps\TicketFlow
copy .env.example .env
copy frontend\.env.example frontend\.env
```

### Run the app (three terminals)

```powershell
# Terminal 1 — database (only the first time per session)
cd C:\PyOps\TicketFlow
docker compose up -d
```

```powershell
# Terminal 2 — backend on port 8080
cd C:\PyOps\TicketFlow
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 3 — frontend on port 5173
cd C:\PyOps\TicketFlow\frontend
npm run dev
```

Open <http://localhost:5173>. The top-bar health indicator should turn green and
the ticket list should load.

### URLs

| Service  | URL                                         |
|----------|---------------------------------------------|
| Frontend | http://localhost:5173                       |
| Backend  | http://localhost:8080                       |
| Health   | http://localhost:8080/health                |
| Swagger  | http://localhost:8080/swagger-ui/index.html |

### If port 8080 is already in use

This is the most common cause of `APPLICATION FAILED TO START / Web server failed
to start. Port 8080 was already in use.` It usually means a previous backend
process from a closed terminal is still running.

Find and stop it:

```powershell
netstat -ano | findstr :8080
# Note the PID in the last column, then:
taskkill /PID <PID> /F
```

Or skip the kill and run the backend on 8081 — the Vite proxy supports this:

```powershell
# Terminal 2 — backend on 8081
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run
```

Add (or edit) `frontend\.env`:

```
VITE_API_BASE_URL=/api
VITE_BACKEND_TARGET=http://localhost:8081
```

Then **restart** `npm run dev` so Vite re-reads the env file.

### If `npm install` says "Could not read package.json"

You are running `npm install` from a directory that has no `package.json`. The
correct path is `C:\PyOps\TicketFlow\frontend`. If that directory is empty or
missing the file, your checkout is stale — see the Quick start at the top.

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

| Layer      | Technology                                |
|------------|-------------------------------------------|
| Backend    | Java 17, Spring Boot 3.5.14, Spring Data JPA |
| Database   | PostgreSQL 16 (Docker Compose, host port 5433) |
| Migrations | Flyway                                    |
| Tests      | JUnit 5, Spring Boot Test, H2 (test)      |
| API docs   | Springdoc OpenAPI / Swagger UI            |
| Frontend   | React 18, TypeScript, Vite 5              |

## macOS / Linux equivalents

The Quick start uses Windows PowerShell. On macOS/Linux/Git Bash use `./mvnw`
instead of `.\mvnw.cmd`, and `cp` instead of `copy`. Setting an env var becomes:

```bash
SERVER_PORT=8081 ./mvnw spring-boot:run
```

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

For non-default backend port handling, see "If port 8080 is already in use" in the Quick start above.

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

The most common first-run failures and their one-line fixes:

| Symptom | Fix |
|---------|-----|
| `npm error enoent ... \frontend\package.json` | You are on a stale checkout. Run `git fetch origin && git checkout claude/inspect-ticketflow-bVddW && git pull`. |
| Backend log shows `spring-boot:3.2.5:run` or `Found 2 JPA repository interfaces` | Same as above — your local code is from before the upgrade. |
| `Web server failed to start. Port 8080 was already in use.` | Either stop the existing process (`netstat -ano \| findstr :8080` then `taskkill /PID <PID> /F`) or run on 8081 with `$env:SERVER_PORT="8081"; .\mvnw.cmd spring-boot:run` and set `VITE_BACKEND_TARGET=http://localhost:8081` in `frontend\.env`. |
| Top bar shows "Backend offline" | Open `http://localhost:8080/health` directly. If that works, restart `npm run dev` so Vite picks up `frontend\.env`. |
| `UnsupportedClassVersionError` on `mvnw spring-boot:run` | Stale `target/`. Run `.\mvnw.cmd clean` and try again. Confirm `pom.xml` pins both `<java.version>17</java.version>` and `<maven.compiler.release>17</maven.compiler.release>`. |
| Flyway warning: `Schema "public" has a version (2) that is newer than the latest available migration (1)` | Your local DB has migrations from a newer code version, but you ran an older code version. Pull latest, or wipe the DB volume: `docker compose down -v && docker compose up -d`. |

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