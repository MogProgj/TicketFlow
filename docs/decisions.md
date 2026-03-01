# Architecture Decision Records

> Public build started on 2026-03-01

---

## ADR 001: PostgreSQL + Flyway

**Status:** Accepted

**Context:**
TicketFlow needs a relational database with strong constraint support and a reliable way to manage schema changes over time. The project also needs to be easy to run locally with minimal setup.

**Decision:**
Use PostgreSQL as the primary database, run it via Docker Compose for local development, and manage all schema migrations through Flyway.

**Consequences:**
- Schema changes are versioned and repeatable across environments.
- Local setup requires only Docker and Java.
- PostgreSQL `CHECK` constraints enforce data integrity at the database level (e.g. priority and status values).

---

## ADR 002: Status workflow enforced in service layer

**Status:** Accepted

**Context:**
Tickets follow a status lifecycle: OPEN → IN_PROGRESS → WAITING → RESOLVED → CLOSED. Allowing arbitrary status changes would defeat the purpose of a structured workflow.

**Decision:**
Status transition rules will be enforced in the Java service layer rather than at the database level. The database stores the current status, but the application validates that each transition is allowed before persisting it.

**Consequences:**
- Transition logic is testable in isolation with unit tests.
- The service layer acts as the single source of truth for workflow rules.
- The database `CHECK` constraint still guards against invalid status values, but does not enforce ordering.
