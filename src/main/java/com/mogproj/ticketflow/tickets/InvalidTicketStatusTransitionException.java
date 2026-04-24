package com.mogproj.ticketflow.tickets;

public class InvalidTicketStatusTransitionException extends RuntimeException {

    public InvalidTicketStatusTransitionException(Ticket.Status currentStatus, Ticket.Status nextStatus) {
        super("Invalid status transition from " + currentStatus + " to " + nextStatus + ".");
    }
}
