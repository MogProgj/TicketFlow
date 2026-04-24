package com.mogproj.ticketflow.tickets;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long ticketId) {
        super("Ticket " + ticketId + " was not found.");
    }
}
