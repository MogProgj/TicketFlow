# Schema

## tickets

Core ticket record. Each ticket has a title, description, assignee, and timestamps.

- **priority** is constrained to `P1`, `P2`, `P3`, `P4` via a `CHECK` constraint.
- **status** is constrained to `OPEN`, `IN_PROGRESS`, `WAITING`, `RESOLVED`, `CLOSED`.
- Indexes on `status` and `priority` support filtered queries.

## ticket_comments

Freeform discussion attached to a ticket. Each comment records an author, body, and timestamp. Comments are deleted when the parent ticket is removed (`ON DELETE CASCADE`).

## ticket_events

Audit trail of changes to a ticket, especially status transitions.

This table exists because real support platforms need to answer "who changed what, and when." Without it, status changes are silent and unaccountable. Each event records the actor, event type, previous value, and new value, making it straightforward to reconstruct the full history of a ticket.
