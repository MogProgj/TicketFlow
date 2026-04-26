package com.mogproj.ticketflow.tickets.dto;

import java.time.Instant;

public record TicketCommentResponse(Long id, Long ticketId, String author, String body, Instant createdAt) {
}
