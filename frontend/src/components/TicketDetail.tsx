import { useEffect, useState } from 'react'
import { ApiError, NetworkError, updateTicket } from '../api/client'
import type { Ticket, TicketPriority, TicketStatus, UpdateTicketPayload } from '../api/types'

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

const PRIORITIES: TicketPriority[] = ['P1', 'P2', 'P3', 'P4']

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

function describeError(err: unknown, fallback: string): string {
    if (err instanceof ApiError || err instanceof NetworkError) return err.message
    return fallback
}

export default function TicketDetail({ ticket, loading, error, onUpdated }: Props) {
    const [transitionError, setTransitionError] = useState<string | null>(null)
    const [transitioning, setTransitioning] = useState<TicketStatus | null>(null)

    const [editing, setEditing] = useState(false)
    const [editTitle, setEditTitle] = useState('')
    const [editDescription, setEditDescription] = useState('')
    const [editPriority, setEditPriority] = useState<TicketPriority>('P3')
    const [editAssignee, setEditAssignee] = useState('')
    const [saving, setSaving] = useState(false)
    const [editError, setEditError] = useState<string | null>(null)

    // Reset edit state whenever the selected ticket changes.
    useEffect(() => {
        setEditing(false)
        setEditError(null)
        setTransitionError(null)
    }, [ticket?.id])

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
            setTransitionError(describeError(e, 'Status update failed.'))
        } finally {
            setTransitioning(null)
        }
    }

    const beginEdit = () => {
        setEditTitle(ticket.title)
        setEditDescription(ticket.description)
        setEditPriority(ticket.priority)
        setEditAssignee(ticket.assignee ?? '')
        setEditError(null)
        setEditing(true)
    }

    const cancelEdit = () => {
        setEditing(false)
        setEditError(null)
    }

    const saveEdit = async (e: React.FormEvent) => {
        e.preventDefault()
        const trimmedTitle = editTitle.trim()
        const trimmedDescription = editDescription.trim()
        if (!trimmedTitle || !trimmedDescription) {
            setEditError('Title and description are required.')
            return
        }
        const trimmedAssignee = editAssignee.trim()
        const payload: UpdateTicketPayload = {
            title: trimmedTitle,
            description: trimmedDescription,
            priority: editPriority,
            assignee: trimmedAssignee === '' ? '' : trimmedAssignee,
        }
        setSaving(true)
        setEditError(null)
        try {
            const updated = await updateTicket(ticket.id, payload)
            onUpdated(updated)
            setEditing(false)
        } catch (err) {
            setEditError(describeError(err, 'Failed to save ticket.'))
        } finally {
            setSaving(false)
        }
    }

    if (editing) {
        return (
            <div className="panel detail-panel">
                <div className="panel-header">
                    <div className="detail-id">#{ticket.id}</div>
                    <div className="detail-badges">
                        <span className={`badge status-${ticket.status}`}>{STATUS_LABELS[ticket.status]}</span>
                    </div>
                </div>
                <form className="create-form" onSubmit={saveEdit}>
                    {editError && <div className="form-error">{editError}</div>}

                    <label className="field">
                        <span className="field-label">Title <span className="required">*</span></span>
                        <input
                            className="input"
                            type="text"
                            value={editTitle}
                            onChange={e => setEditTitle(e.target.value)}
                            required
                            disabled={saving}
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Description <span className="required">*</span></span>
                        <textarea
                            className="input textarea"
                            value={editDescription}
                            onChange={e => setEditDescription(e.target.value)}
                            required
                            disabled={saving}
                            rows={4}
                        />
                    </label>

                    <div className="field-row">
                        <label className="field">
                            <span className="field-label">Priority</span>
                            <select
                                className="select"
                                value={editPriority}
                                onChange={e => setEditPriority(e.target.value as TicketPriority)}
                                disabled={saving}
                            >
                                {PRIORITIES.map(p => (
                                    <option key={p} value={p}>{p}</option>
                                ))}
                            </select>
                        </label>
                        <label className="field">
                            <span className="field-label">Assignee</span>
                            <input
                                className="input"
                                type="text"
                                value={editAssignee}
                                onChange={e => setEditAssignee(e.target.value)}
                                disabled={saving}
                                placeholder="Leave blank to clear"
                            />
                        </label>
                    </div>

                    <div className="form-actions">
                        <button type="button" className="btn btn-ghost" onClick={cancelEdit} disabled={saving}>
                            Cancel
                        </button>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Saving…' : 'Save'}
                        </button>
                    </div>
                </form>
            </div>
        )
    }

    return (
        <div className="panel detail-panel">
            <div className="panel-header">
                <div className="detail-id">#{ticket.id}</div>
                <div className="detail-badges">
                    <span className={`badge priority-${ticket.priority}`}>{ticket.priority}</span>
                    <span className={`badge status-${ticket.status}`}>{STATUS_LABELS[ticket.status]}</span>
                    <button className="btn btn-ghost" onClick={beginEdit}>Edit</button>
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
