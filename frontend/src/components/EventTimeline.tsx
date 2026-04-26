import type { TicketEvent, TicketEventType } from '../api/types'

const EVENT_ICONS: Record<TicketEventType, string> = {
    CREATED: '✦',
    STATUS_CHANGED: '⇄',
    COMMENT_ADDED: '💬',
    PRIORITY_CHANGED: '⚑',
    ASSIGNEE_CHANGED: '👤',
    TITLE_CHANGED: '✎',
    DESCRIPTION_CHANGED: '✎',
}

const EVENT_LABELS: Record<TicketEventType, string> = {
    CREATED: 'Ticket created',
    STATUS_CHANGED: 'Status changed',
    COMMENT_ADDED: 'Comment added',
    PRIORITY_CHANGED: 'Priority changed',
    ASSIGNEE_CHANGED: 'Assignee changed',
    TITLE_CHANGED: 'Title updated',
    DESCRIPTION_CHANGED: 'Description updated',
}

interface Props {
    events: TicketEvent[]
}

function formatDate(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
    })
}

function renderChange(event: TicketEvent): string | null {
    const { eventType, fromValue, toValue } = event
    if (eventType === 'CREATED') return toValue ? `Initial status: ${toValue}` : null
    if (fromValue !== null && toValue !== null) return `${fromValue} → ${toValue}`
    if (toValue !== null) return `→ ${toValue}`
    if (fromValue !== null) return `${fromValue} → (cleared)`
    return null
}

export default function EventTimeline({ events }: Props) {
    return (
        <section className="panel timeline-panel">
            <h3 className="panel-section-title">Activity Timeline <span className="count-badge">{events.length}</span></h3>

            {events.length === 0 && <div className="state-message">No events yet.</div>}

            <ol className="timeline">
                {events.map(event => {
                    const change = renderChange(event)
                    return (
                        <li key={event.id} className={`timeline-item event-${event.eventType}`}>
                            <div className="timeline-icon">{EVENT_ICONS[event.eventType]}</div>
                            <div className="timeline-content">
                                <div className="timeline-top">
                                    <span className="timeline-label">{EVENT_LABELS[event.eventType]}</span>
                                    <span className="timeline-actor">{event.actor}</span>
                                    <span className="timeline-time">{formatDate(event.createdAt)}</span>
                                </div>
                                {change && <div className="timeline-change">{change}</div>}
                            </div>
                        </li>
                    )
                })}
            </ol>
        </section>
    )
}
