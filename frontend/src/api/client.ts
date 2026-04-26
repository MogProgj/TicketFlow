import type {
    CreateCommentPayload,
    CreateTicketPayload,
    HealthResponse,
    ListTicketsParams,
    Ticket,
    TicketComment,
    TicketEvent,
    UpdateTicketPayload,
} from './types'

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

class ApiError extends Error {
    constructor(
        public readonly status: number,
        message: string,
    ) {
        super(message)
        this.name = 'ApiError'
    }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const res = await fetch(`${BASE_URL}${path}`, {
        headers: { 'Content-Type': 'application/json', ...init?.headers },
        ...init,
    })

    if (!res.ok) {
        let message = `Request failed: ${res.status} ${res.statusText}`
        try {
            const body = (await res.json()) as { message?: string }
            if (body.message) message = body.message
        } catch {
            // ignore parse errors
        }
        throw new ApiError(res.status, message)
    }

    const text = await res.text()
    return text ? (JSON.parse(text) as T) : ({} as T)
}

export async function getHealth(): Promise<HealthResponse> {
    return request<HealthResponse>('/health')
}

export async function listTickets(params: ListTicketsParams = {}): Promise<Ticket[]> {
    const qs = new URLSearchParams()
    if (params.status) qs.set('status', params.status)
    if (params.priority) qs.set('priority', params.priority)
    if (params.q) qs.set('q', params.q)
    const query = qs.toString() ? `?${qs.toString()}` : ''
    return request<Ticket[]>(`/tickets${query}`)
}

export async function createTicket(payload: CreateTicketPayload): Promise<Ticket> {
    return request<Ticket>('/tickets', {
        method: 'POST',
        body: JSON.stringify(payload),
    })
}

export async function getTicket(id: number): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`)
}

export async function updateTicket(id: number, payload: UpdateTicketPayload): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`, {
        method: 'PATCH',
        body: JSON.stringify(payload),
    })
}

export async function listComments(ticketId: number): Promise<TicketComment[]> {
    return request<TicketComment[]>(`/tickets/${ticketId}/comments`)
}

export async function createComment(ticketId: number, payload: CreateCommentPayload): Promise<TicketComment> {
    return request<TicketComment>(`/tickets/${ticketId}/comments`, {
        method: 'POST',
        body: JSON.stringify(payload),
    })
}

export async function listEvents(ticketId: number): Promise<TicketEvent[]> {
    return request<TicketEvent[]>(`/tickets/${ticketId}/events`)
}

export { ApiError }
