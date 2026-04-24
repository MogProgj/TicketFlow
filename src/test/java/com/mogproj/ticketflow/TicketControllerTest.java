package com.mogproj.ticketflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogproj.ticketflow.tickets.Ticket;
import com.mogproj.ticketflow.tickets.TicketEventRepository;
import com.mogproj.ticketflow.tickets.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketEventRepository ticketEventRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketEventRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
    }

    @Test
    void createTicketCreatesOpenTicketAndCreatedEvent() throws Exception {
        mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Login page returns 500",
                          "description": "Users hit an internal server error on login.",
                          "priority": "P1",
                          "assignee": "alice"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Login page returns 500"))
                .andExpect(jsonPath("$.priority").value("P1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignee").value("alice"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void getTicketByIdReturnsTicket() throws Exception {
        Ticket ticket = persistTicket("API is timing out", "The list endpoint responds slowly.", Ticket.Priority.P2,
                Ticket.Status.OPEN, "ops");

        mockMvc.perform(get("/tickets/{ticketId}", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.title").value("API is timing out"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void listTicketsReturnsAllTickets() throws Exception {
        persistTicket("First issue", "First description", Ticket.Priority.P3, Ticket.Status.OPEN, null);
        persistTicket("Second issue", "Second description", Ticket.Priority.P4, Ticket.Status.WAITING, null);

        mockMvc.perform(get("/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listTicketsCanFilterByStatus() throws Exception {
        persistTicket("Open issue", "Still open", Ticket.Priority.P3, Ticket.Status.OPEN, null);
        persistTicket("Waiting issue", "Waiting on input", Ticket.Priority.P2, Ticket.Status.WAITING, null);

        mockMvc.perform(get("/tickets").param("status", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void updateTicketStatusAllowsValidTransition() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "IN_PROGRESS"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void updateTicketStatusRejectsInvalidTransition() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "CLOSED"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition from OPEN to CLOSED."));
    }

    @Test
    void getTicketByIdReturnsNotFoundForMissingTicket() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 9999 was not found."));
    }

    @Test
    void getTicketEventsReturnsCreateAndStatusChangeEventsInOrder() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "status": "IN_PROGRESS"
                        }
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$[0].actor").value("system"))
                .andExpect(jsonPath("$[0].fromValue").value(nullValue()))
                .andExpect(jsonPath("$[0].toValue").value("OPEN"))
                .andExpect(jsonPath("$[1].eventType").value("STATUS_CHANGED"))
                .andExpect(jsonPath("$[1].fromValue").value("OPEN"))
                .andExpect(jsonPath("$[1].toValue").value("IN_PROGRESS"));
    }

    private Long createTicketAndReturnId() throws Exception {
        String responseBody = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Checkout flow is broken",
                          "description": "Customers cannot complete checkout.",
                          "priority": "P1",
                          "assignee": "triage"
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        return response.path("id").asLong();
    }

    private Ticket persistTicket(String title, String description, Ticket.Priority priority, Ticket.Status status,
            String assignee) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setAssignee(assignee);
        return ticketRepository.saveAndFlush(ticket);
    }
}
