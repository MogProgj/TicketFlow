import { useCallback, useEffect, useState } from 'react'
import {
    ApiError,
    NetworkError,
    getApiBaseUrl,
    getHealth,
    getTicket,
    listComments,
    listEvents,
    listTickets,
} from './api/client'
import type { ListTicketsParams, Ticket, TicketComment, TicketEvent } from './api/types'
import CommentsPanel from './components/CommentsPanel'
import CreateTicketForm from './components/CreateTicketForm'
import EventTimeline from './components/EventTimeline'
import TicketDetail from './components/TicketDetail'
import TicketList from './components/TicketList'
import TopBar from './components/TopBar'

type HealthStatus = 'checking' | 'ok' | 'offline'

function describeError(err: unknown, fallback: string): string {
    if (err instanceof ApiError || err instanceof NetworkError) return err.message
    return fallback
}

export default function App() {
    const [healthStatus, setHealthStatus] = useState<HealthStatus>('checking')
    const [tickets, setTickets] = useState<Ticket[]>([])
    const [ticketsLoading, setTicketsLoading] = useState(true)
    const [ticketsError, setTicketsError] = useState<string | null>(null)
    const [filters, setFilters] = useState<ListTicketsParams>({ status: '', priority: '', q: '' })

    const [selectedId, setSelectedId] = useState<number | null>(null)
    const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null)
    const [detailLoading, setDetailLoading] = useState(false)

    const [comments, setComments] = useState<TicketComment[]>([])
    const [events, setEvents] = useState<TicketEvent[]>([])
    const [detailError, setDetailError] = useState<string | null>(null)

    const [showCreateForm, setShowCreateForm] = useState(false)

    const apiBase = getApiBaseUrl()

    const checkHealth = useCallback(() => {
        setHealthStatus(prev => (prev === 'ok' ? prev : 'checking'))
        getHealth()
            .then(() => setHealthStatus('ok'))
            .catch(() => setHealthStatus('offline'))
    }, [])

    useEffect(() => {
        checkHealth()
        const id = setInterval(checkHealth, 30_000)
        return () => clearInterval(id)
    }, [checkHealth])

    const loadTickets = useCallback((params: ListTicketsParams) => {
        setTicketsLoading(true)
        setTicketsError(null)
        listTickets(params)
            .then(setTickets)
            .catch((e: unknown) => {
                setTicketsError(describeError(e, 'Failed to load tickets.'))
                setTickets([])
            })
            .finally(() => setTicketsLoading(false))
    }, [])

    useEffect(() => {
        loadTickets(filters)
    }, [filters, loadTickets])

    const loadDetail = useCallback((id: number) => {
        setDetailLoading(true)
        setDetailError(null)
        Promise.all([getTicket(id), listComments(id), listEvents(id)])
            .then(([ticket, ticketComments, ticketEvents]) => {
                setSelectedTicket(ticket)
                setComments(ticketComments)
                setEvents(ticketEvents)
            })
            .catch((e: unknown) => {
                setDetailError(describeError(e, 'Failed to load ticket detail.'))
            })
            .finally(() => setDetailLoading(false))
    }, [])

    const handleSelectTicket = (id: number) => {
        setShowCreateForm(false)
        setSelectedId(id)
        loadDetail(id)
    }

    const handleTicketCreated = (ticket: Ticket) => {
        setShowCreateForm(false)
        loadTickets(filters)
        setSelectedId(ticket.id)
        loadDetail(ticket.id)
    }

    const handleTicketUpdated = (ticket: Ticket) => {
        setSelectedTicket(ticket)
        listEvents(ticket.id).then(setEvents).catch(() => undefined)
        setTickets(prev => prev.map(t => (t.id === ticket.id ? ticket : t)))
        loadTickets(filters)
    }

    const handleCommentAdded = () => {
        if (selectedId !== null) {
            listComments(selectedId).then(setComments).catch(() => undefined)
            listEvents(selectedId).then(setEvents).catch(() => undefined)
        }
    }

    const handleRefresh = () => {
        checkHealth()
        loadTickets(filters)
        if (selectedId !== null) loadDetail(selectedId)
    }

    return (
        <div className="app">
            <TopBar
                healthStatus={healthStatus}
                apiBase={apiBase}
                onRefresh={handleRefresh}
                onNewTicket={() => setShowCreateForm(true)}
            />

            <div className="workspace">
                <aside className="sidebar">
                    <TicketList
                        tickets={tickets}
                        loading={ticketsLoading}
                        error={ticketsError}
                        filters={filters}
                        selectedId={selectedId}
                        onFilterChange={setFilters}
                        onSelect={handleSelectTicket}
                    />
                </aside>

                <main className="main-panel">
                    {showCreateForm && (
                        <CreateTicketForm
                            onCreated={handleTicketCreated}
                            onCancel={() => setShowCreateForm(false)}
                        />
                    )}

                    {!showCreateForm && (
                        <>
                            <TicketDetail
                                ticket={selectedTicket}
                                loading={detailLoading}
                                error={detailError}
                                onUpdated={handleTicketUpdated}
                            />
                            {selectedTicket && (
                                <div className="detail-extras">
                                    <CommentsPanel
                                        ticketId={selectedTicket.id}
                                        comments={comments}
                                        onCommentAdded={handleCommentAdded}
                                    />
                                    <EventTimeline events={events} />
                                </div>
                            )}
                        </>
                    )}
                </main>
            </div>
        </div>
    )
}
