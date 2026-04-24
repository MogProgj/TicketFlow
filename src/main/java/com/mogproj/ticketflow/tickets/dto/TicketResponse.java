package com.mogproj.ticketflow.tickets.dto;

import java.time.Instant;

import com.mogproj.ticketflow.tickets.Ticket;

public record TicketResponse(
        Long id,
        String title,
        String description,
        Ticket.Priority priority,
        Ticket.Status status,
        String assignee,
        Instant createdAt,
        Instant updatedAt) {
}
