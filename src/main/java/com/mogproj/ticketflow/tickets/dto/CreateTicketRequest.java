package com.mogproj.ticketflow.tickets.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mogproj.ticketflow.tickets.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@JsonIgnoreProperties(ignoreUnknown = false)
public class CreateTicketRequest {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "description must not be blank")
    private String description;

    @NotNull(message = "priority is required")
    private Ticket.Priority priority;

    @Pattern(regexp = ".*\\S.*", message = "assignee must not be blank")
    private String assignee;

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
}
