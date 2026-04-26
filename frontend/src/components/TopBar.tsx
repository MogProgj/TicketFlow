type HealthStatus = 'checking' | 'ok' | 'offline'

interface Props {
    healthStatus: HealthStatus
    apiBase: string
    onRefresh: () => void
    onNewTicket: () => void
}

export default function TopBar({ healthStatus, apiBase, onRefresh, onNewTicket }: Props) {
    const indicator =
        healthStatus === 'ok'
            ? { label: 'Backend online', cls: 'health-dot health-ok' }
            : healthStatus === 'offline'
                ? { label: 'Backend offline', cls: 'health-dot health-offline' }
                : { label: 'Checking…', cls: 'health-dot health-checking' }

    const tooltip = `${indicator.label} · API base: ${apiBase}`

    return (
        <header className="topbar">
            <div className="topbar-brand">
                <span className="topbar-logo">T</span>
                <span className="topbar-title">TicketFlow</span>
            </div>
            <div className="topbar-right">
                <span className="health-indicator" title={tooltip}>
                    <span className={indicator.cls} />
                    <span className="health-label">{indicator.label}</span>
                </span>
                <button className="btn btn-ghost" onClick={onRefresh} title="Refresh data">
                    Refresh
                </button>
                <button className="btn btn-primary" onClick={onNewTicket}>
                    + New Ticket
                </button>
            </div>
        </header>
    )
}
