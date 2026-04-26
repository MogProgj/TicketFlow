import type { ListTicketsParams, Ticket, TicketPriority, TicketStatus } from '../api/types'

const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'WAITING', 'RESOLVED', 'CLOSED']
const PRIORITIES: TicketPriority[] = ['P1', 'P2', 'P3', 'P4']

interface Props {
    tickets: Ticket[]
    loading: boolean
    error: string | null
    filters: ListTicketsParams
    selectedId: number | null
    onFilterChange: (params: ListTicketsParams) => void
    onSelect: (id: number) => void
}

function formatAge(iso: string): string {
    const ms = Date.now() - new Date(iso).getTime()
    const mins = Math.floor(ms / 60_000)
    if (mins < 60) return `${mins}m ago`
    const hrs = Math.floor(mins / 60)
    if (hrs < 24) return `${hrs}h ago`
    return `${Math.floor(hrs / 24)}d ago`
}

export default function TicketList({ tickets, loading, error, filters, selectedId, onFilterChange, onSelect }: Props) {
    return (
        <div className="ticket-list-panel">
            <div className="ticket-list-filters">
                <input
                    className="input filter-search"
                    type="text"
                    placeholder="Search tickets…"
                    value={filters.q ?? ''}
                    onChange={e => onFilterChange({ ...filters, q: e.target.value })}
                />
                <div className="filter-row">
                    <select
                        className="select"
                        value={filters.status ?? ''}
                        onChange={e => onFilterChange({ ...filters, status: e.target.value as TicketStatus | '' })}
                    >
                        <option value="">All statuses</option>
                        {STATUSES.map(s => (
                            <option key={s} value={s}>{s.replace('_', ' ')}</option>
                        ))}
                    </select>
                    <select
                        className="select"
                        value={filters.priority ?? ''}
                        onChange={e => onFilterChange({ ...filters, priority: e.target.value as TicketPriority | '' })}
                    >
                        <option value="">All priorities</option>
                        {PRIORITIES.map(p => (
                            <option key={p} value={p}>{p}</option>
                        ))}
                    </select>
                </div>
            </div>

            <div className="ticket-list-body">
                {loading && <div className="state-message">Loading tickets…</div>}
                {!loading && error && <div className="state-message state-error">{error}</div>}
                {!loading && !error && tickets.length === 0 && (
                    <div className="state-message">No tickets match the current filters.</div>
                )}
                {!loading && !error && tickets.map(ticket => (
                    <button
                        key={ticket.id}
                        className={`ticket-row${selectedId === ticket.id ? ' ticket-row--selected' : ''}`}
                        onClick={() => onSelect(ticket.id)}
                    >
                        <div className="ticket-row-top">
                            <span className={`badge priority-${ticket.priority}`}>{ticket.priority}</span>
                            <span className={`badge status-${ticket.status}`}>{ticket.status.replace('_', ' ')}</span>
                        </div>
                        <div className="ticket-row-title">{ticket.title}</div>
                        <div className="ticket-row-meta">
                            <span>{ticket.assignee ?? 'Unassigned'}</span>
                            <span>{formatAge(ticket.updatedAt)}</span>
                        </div>
                    </button>
                ))}
            </div>
        </div>
    )
}
