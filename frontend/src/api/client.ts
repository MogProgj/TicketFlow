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

const RAW_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api'
const BASE_URL = RAW_BASE_URL.replace(/\/+$/, '')

export function getApiBaseUrl(): string {
    return BASE_URL || '/'
}

class ApiError extends Error {
    constructor(
        public readonly status: number,
        message: string,
        public readonly fieldErrors?: Record<string, string>,
    ) {
        super(message)
        this.name = 'ApiError'
    }
}

class NetworkError extends Error {
    constructor(message: string) {
        super(message)
        this.name = 'NetworkError'
    }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
    const url = `${BASE_URL}${path}`
    let res: Response
    try {
        res = await fetch(url, {
            headers: { 'Content-Type': 'application/json', ...init?.headers },
            ...init,
        })
    } catch {
        throw new NetworkError(
            `Could not reach TicketFlow API at ${BASE_URL || '/'}. ` +
            `Check that the backend is running and that the frontend env points to the correct port.`,
        )
    }

    if (!res.ok) {
        let message = `Request failed: ${res.status} ${res.statusText}`
        let fieldErrors: Record<string, string> | undefined
        const contentType = res.headers.get('content-type') ?? ''
        if (contentType.includes('application/json')) {
            try {
                const body = (await res.json()) as { message?: string; fieldErrors?: Record<string, string> }
                if (body.message) message = body.message
                if (body.fieldErrors && Object.keys(body.fieldErrors).length > 0) {
                    fieldErrors = body.fieldErrors
                    const detail = Object.entries(body.fieldErrors)
                        .map(([k, v]) => `${k}: ${v}`)
                        .join('; ')
                    message = `${message} (${detail})`
                }
            } catch {
                // body wasn't valid JSON, fall back to status text
            }
        } else {
            try {
                const text = await res.text()
                if (text) message = text.slice(0, 300)
            } catch {
                // ignore
            }
        }
        throw new ApiError(res.status, message, fieldErrors)
    }

    if (res.status === 204) {
        return {} as T
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
    if (params.q && params.q.trim()) qs.set('q', params.q.trim())
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

export { ApiError, NetworkError }
