import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, getHealth, getTicket, listComments, listEvents, listTickets } from './api/client'
import type { ListTicketsParams, Ticket, TicketComment, TicketEvent } from './api/types'
import CommentsPanel from './components/CommentsPanel'
import CreateTicketForm from './components/CreateTicketForm'
import EventTimeline from './components/EventTimeline'
import TicketDetail from './components/TicketDetail'
import TicketList from './components/TicketList'
import TopBar from './components/TopBar'

type HealthStatus = 'checking' | 'ok' | 'offline'

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

    // Debounce timer for search input
    const searchTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

    // Health check on mount and every 30s
    useEffect(() => {
        let active = true
        const check = () => {
            getHealth()
                .then(() => { if (active) setHealthStatus('ok') })
                .catch(() => { if (active) setHealthStatus('offline') })
        }
        check()
        const id = setInterval(check, 30_000)
        return () => { active = false; clearInterval(id) }
    }, [])

    const loadTickets = useCallback((params: ListTicketsParams) => {
        setTicketsLoading(true)
        setTicketsError(null)
        listTickets(params)
            .then(setTickets)
            .catch((e: unknown) => {
                setTicketsError(e instanceof ApiError ? e.message : 'Failed to load tickets.')
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
                setDetailError(e instanceof ApiError ? e.message : 'Failed to load ticket detail.')
            })
            .finally(() => setDetailLoading(false))
    }, [])

    const handleSelectTicket = (id: number) => {
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
        // Refresh events after update
        listEvents(ticket.id).then(setEvents).catch(() => undefined)
        // Refresh list (status/priority may have changed)
        loadTickets(filters)
        // Update the ticket in the list inline to avoid full reload flicker
        setTickets(prev => prev.map(t => t.id === ticket.id ? ticket : t))
    }

    const handleCommentAdded = () => {
        if (selectedId !== null) {
            listComments(selectedId).then(setComments).catch(() => undefined)
            listEvents(selectedId).then(setEvents).catch(() => undefined)
        }
    }

    const handleFilterChange = (next: ListTicketsParams) => {
        if (searchTimer.current) clearTimeout(searchTimer.current)
        // Debounce the q param only
        if (next.q !== filters.q && next.q !== '') {
            searchTimer.current = setTimeout(() => setFilters(next), 400)
        } else {
            setFilters(next)
        }
    }

    return (
        <div className="app">
            <TopBar healthStatus={healthStatus} onNewTicket={() => setShowCreateForm(true)} />

            <div className="workspace">
                <aside className="sidebar">
                    <TicketList
                        tickets={tickets}
                        loading={ticketsLoading}
                        error={ticketsError}
                        filters={filters}
                        selectedId={selectedId}
                        onFilterChange={handleFilterChange}
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
