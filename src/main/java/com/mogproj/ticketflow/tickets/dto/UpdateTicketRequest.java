package com.mogproj.ticketflow.tickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mogproj.ticketflow.tickets.Ticket;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateTicketRequest {

    @Pattern(regexp = ".*\\S.*", message = "title must not be blank")
    private String title;

    @Pattern(regexp = ".*\\S.*", message = "description must not be blank")
    private String description;

    private Ticket.Priority priority;

    private String assignee;

    private Ticket.Status status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Ticket.Priority getPriority() {
        return priority;
    }

    public void setPriority(Ticket.Priority priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Ticket.Status getStatus() {
        return status;
    }

    public void setStatus(Ticket.Status status) {
        this.status = status;
    }
}
