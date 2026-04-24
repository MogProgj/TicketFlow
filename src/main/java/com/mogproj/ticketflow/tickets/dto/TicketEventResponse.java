package com.mogproj.ticketflow.tickets.dto;

import java.time.Instant;

import com.mogproj.ticketflow.tickets.TicketEvent;

public record TicketEventResponse(
        Long id,
        TicketEvent.EventType eventType,
        String actor,
        String fromValue,
        String toValue,
        Instant createdAt) {
}
