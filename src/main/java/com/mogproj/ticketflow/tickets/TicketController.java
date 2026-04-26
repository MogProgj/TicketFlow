package com.mogproj.ticketflow.tickets;

import java.util.List;

import com.mogproj.ticketflow.tickets.dto.CreateTicketCommentRequest;
import com.mogproj.ticketflow.tickets.dto.CreateTicketRequest;
import com.mogproj.ticketflow.tickets.dto.TicketCommentResponse;
import com.mogproj.ticketflow.tickets.dto.TicketEventResponse;
import com.mogproj.ticketflow.tickets.dto.TicketResponse;
import com.mogproj.ticketflow.tickets.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
public class TicketController {

  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @PostMapping
  public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request));
  }

  @GetMapping
  public List<TicketResponse> listTickets(
      @RequestParam(required = false) Ticket.Status status,
      @RequestParam(required = false) Ticket.Priority priority,
      @RequestParam(required = false) String q) {
    return ticketService.listTickets(status, priority, q);
  }

  @GetMapping("/{ticketId}")
  public TicketResponse getTicketById(@PathVariable Long ticketId) {
    return ticketService.getTicketById(ticketId);
  }

  @PatchMapping("/{ticketId}")
  public TicketResponse updateTicket(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
    return ticketService.updateTicket(ticketId, request);
  }

  @GetMapping("/{ticketId}/events")
  public List<TicketEventResponse> getTicketEvents(@PathVariable Long ticketId) {
    return ticketService.getTicketEvents(ticketId);
  }

  @PostMapping("/{ticketId}/comments")
  public ResponseEntity<TicketCommentResponse> createComment(
      @PathVariable Long ticketId,
      @Valid @RequestBody CreateTicketCommentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createComment(ticketId, request));
  }

  @GetMapping("/{ticketId}/comments")
  public List<TicketCommentResponse> listComments(@PathVariable Long ticketId) {
    return ticketService.listComments(ticketId);
  }
}
