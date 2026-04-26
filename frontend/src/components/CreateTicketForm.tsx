import { useState } from 'react'
import { ApiError, NetworkError, createTicket } from '../api/client'
import type { CreateTicketPayload, Ticket, TicketPriority } from '../api/types'

interface Props {
    onCreated: (ticket: Ticket) => void
    onCancel: () => void
}

const PRIORITIES: TicketPriority[] = ['P1', 'P2', 'P3', 'P4']

export default function CreateTicketForm({ onCreated, onCancel }: Props) {
    const [title, setTitle] = useState('')
    const [description, setDescription] = useState('')
    const [priority, setPriority] = useState<TicketPriority>('P3')
    const [assignee, setAssignee] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setError(null)
        setSubmitting(true)

        const payload: CreateTicketPayload = {
            title: title.trim(),
            description: description.trim(),
            priority,
            assignee: assignee.trim() || null,
        }

        try {
            const ticket = await createTicket(payload)
            onCreated(ticket)
        } catch (err) {
            if (err instanceof ApiError || err instanceof NetworkError) setError(err.message)
            else setError('Failed to create ticket.')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="create-form-container">
            <div className="panel">
                <div className="panel-header">
                    <h2 className="panel-title">New Ticket</h2>
                    <button className="btn btn-ghost" onClick={onCancel} disabled={submitting}>
                        Cancel
                    </button>
                </div>

                <form className="create-form" onSubmit={handleSubmit}>
                    {error && <div className="form-error">{error}</div>}

                    <label className="field">
                        <span className="field-label">Title <span className="required">*</span></span>
                        <input
                            className="input"
                            type="text"
                            value={title}
                            onChange={e => setTitle(e.target.value)}
                            required
                            disabled={submitting}
                            placeholder="Brief summary of the issue"
                        />
                    </label>

                    <label className="field">
                        <span className="field-label">Description <span className="required">*</span></span>
                        <textarea
                            className="input textarea"
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            required
                            disabled={submitting}
                            rows={4}
                            placeholder="Describe the issue in detail"
                        />
                    </label>

                    <div className="field-row">
                        <label className="field">
                            <span className="field-label">Priority <span className="required">*</span></span>
                            <select
                                className="select"
                                value={priority}
                                onChange={e => setPriority(e.target.value as TicketPriority)}
                                disabled={submitting}
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
                                value={assignee}
                                onChange={e => setAssignee(e.target.value)}
                                disabled={submitting}
                                placeholder="Optional"
                            />
                        </label>
                    </div>

                    <div className="form-actions">
                        <button className="btn btn-primary" type="submit" disabled={submitting}>
                            {submitting ? 'Creating…' : 'Create Ticket'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    )
}
