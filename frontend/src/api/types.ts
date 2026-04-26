export type TicketPriority = 'P1' | 'P2' | 'P3' | 'P4'

export type TicketStatus =
    | 'OPEN'
    | 'IN_PROGRESS'
    | 'WAITING'
    | 'RESOLVED'
    | 'CLOSED'

export interface Ticket {
    id: number
    title: string
    description: string
    priority: TicketPriority
    status: TicketStatus
    assignee: string | null
    createdAt: string
    updatedAt: string
}

export interface TicketComment {
    id: number
    ticketId: number
    author: string
    body: string
    createdAt: string
}

export type TicketEventType =
    | 'CREATED'
    | 'STATUS_CHANGED'
    | 'COMMENT_ADDED'
    | 'PRIORITY_CHANGED'
    | 'ASSIGNEE_CHANGED'
    | 'TITLE_CHANGED'
    | 'DESCRIPTION_CHANGED'

export interface TicketEvent {
    id: number
    eventType: TicketEventType
    actor: string
    fromValue: string | null
    toValue: string | null
    createdAt: string
}

export interface CreateTicketPayload {
    title: string
    description: string
    priority: TicketPriority
    assignee?: string | null
}

export interface UpdateTicketPayload {
    title?: string
    description?: string
    priority?: TicketPriority
    status?: TicketStatus
    assignee?: string | null
}

export interface CreateCommentPayload {
    author: string
    body: string
}

export interface ListTicketsParams {
    status?: TicketStatus | ''
    priority?: TicketPriority | ''
    q?: string
}

export interface HealthResponse {
    status: string
}
