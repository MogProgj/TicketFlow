import { useState } from 'react'
import { ApiError, updateTicket } from '../api/client'
import type { Ticket, TicketStatus } from '../api/types'

const STATUS_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
    OPEN: ['IN_PROGRESS'],
    IN_PROGRESS: ['WAITING', 'RESOLVED'],
    WAITING: ['IN_PROGRESS', 'RESOLVED'],
    RESOLVED: ['CLOSED'],
    CLOSED: [],
}

const STATUS_LABELS: Record<TicketStatus, string> = {
    OPEN: 'Open',
    IN_PROGRESS: 'In Progress',
    WAITING: 'Waiting',
    RESOLVED: 'Resolved',
    CLOSED: 'Closed',
}

interface Props {
    ticket: Ticket | null
    loading: boolean
    error: string | null
    onUpdated: (ticket: Ticket) => void
}

function formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    })
}

export default function TicketDetail({ ticket, loading, error, onUpdated }: Props) {
    const [transitionError, setTransitionError] = useState<string | null>(null)
    const [transitioning, setTransitioning] = useState<TicketStatus | null>(null)

    if (loading) return <div className="panel detail-empty"><div className="state-message">Loading…</div></div>
    if (error) return <div className="panel detail-empty"><div className="state-message state-error">{error}</div></div>
    if (!ticket) {
        return (
            <div className="panel detail-empty">
                <div className="empty-state">
                    <div className="empty-state-icon">✦</div>
                    <div className="empty-state-title">No ticket selected</div>
                    <div className="empty-state-hint">Select a ticket from the list or create a new one.</div>
                </div>
            </div>
        )
    }

    const allowedTransitions = STATUS_TRANSITIONS[ticket.status]

    const handleTransition = async (next: TicketStatus) => {
        setTransitionError(null)
        setTransitioning(next)
        try {
            const updated = await updateTicket(ticket.id, { status: next })
            onUpdated(updated)
        } catch (e) {
            setTransitionError(e instanceof ApiError ? e.message : 'Status update failed.')
        } finally {
            setTransitioning(null)
        }
    }

    return (
        <div className="panel detail-panel">
            <div className="panel-header">
                <div className="detail-id">#{ticket.id}</div>
                <div className="detail-badges">
                    <span className={`badge priority-${ticket.priority}`}>{ticket.priority}</span>
                    <span className={`badge status-${ticket.status}`}>{STATUS_LABELS[ticket.status]}</span>
                </div>
            </div>

            <h1 className="detail-title">{ticket.title}</h1>
            <p className="detail-description">{ticket.description}</p>

            <dl className="detail-meta">
                <dt>Assignee</dt>
                <dd>{ticket.assignee ?? <span className="muted">Unassigned</span>}</dd>
                <dt>Created</dt>
                <dd>{formatDate(ticket.createdAt)}</dd>
                <dt>Updated</dt>
                <dd>{formatDate(ticket.updatedAt)}</dd>
            </dl>

            {allowedTransitions.length > 0 && (
                <div className="status-actions">
                    <span className="status-actions-label">Move to:</span>
                    {allowedTransitions.map(next => (
                        <button
                            key={next}
                            className={`btn btn-status status-${next}`}
                            onClick={() => handleTransition(next)}
                            disabled={transitioning !== null}
                        >
                            {transitioning === next ? 'Moving…' : STATUS_LABELS[next]}
                        </button>
                    ))}
                </div>
            )}

            {transitionError && <div className="form-error">{transitionError}</div>}
        </div>
    )
}
