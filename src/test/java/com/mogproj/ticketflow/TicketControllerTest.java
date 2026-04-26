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
                Ticket ticket = persistTicket("API is timing out", "The list endpoint responds slowly.",
                                Ticket.Priority.P2,
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
                                .andExpect(jsonPath("$.message")
                                                .value("Invalid status transition from OPEN to CLOSED."));
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

        @Test
        void listTicketsCanFilterByPriority() throws Exception {
                persistTicket("P1 issue", "Critical bug", Ticket.Priority.P1, Ticket.Status.OPEN, null);
                persistTicket("P3 issue", "Minor bug", Ticket.Priority.P3, Ticket.Status.OPEN, null);

                mockMvc.perform(get("/tickets").param("priority", "P1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].priority").value("P1"));
        }

        @Test
        void listTicketsCanSearchByQueryInTitle() throws Exception {
                persistTicket("Login page returns 500", "Users hit an error on login.", Ticket.Priority.P1,
                                Ticket.Status.OPEN, null);
                persistTicket("Unrelated issue", "Nothing about the login.", Ticket.Priority.P3, Ticket.Status.OPEN,
                                null);

                mockMvc.perform(get("/tickets").param("q", "login"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void listTicketsSearchIsCaseInsensitive() throws Exception {
                persistTicket("Checkout flow is broken", "Users cannot complete CHECKOUT.", Ticket.Priority.P1,
                                Ticket.Status.OPEN, null);
                persistTicket("Unrelated issue", "Nothing here.", Ticket.Priority.P3, Ticket.Status.OPEN, null);

                mockMvc.perform(get("/tickets").param("q", "checkout"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].title").value("Checkout flow is broken"));
        }

        @Test
        void invalidPriorityQueryParamReturns400() throws Exception {
                mockMvc.perform(get("/tickets").param("priority", "INVALID"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void invalidStatusEnumInPatchBodyReturns400() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "status": "NOT_A_STATUS"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void unknownJsonFieldOnCreateReturns400() throws Exception {
                mockMvc.perform(post("/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "Test ticket",
                                                  "description": "A test.",
                                                  "priority": "P2",
                                                  "unknownField": "should be rejected"
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Unknown field 'unknownField'."));
        }

        @Test
        void createTicketWithNoAssigneeStoresNull() throws Exception {
                mockMvc.perform(post("/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "No assignee ticket",
                                                  "description": "This ticket has no assignee.",
                                                  "priority": "P3"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.assignee").value(nullValue()));
        }

        @Test
        void updatePriorityEmitsPriorityChangedEvent() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "priority": "P2"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.priority").value("P2"));

                mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[1].eventType").value("PRIORITY_CHANGED"))
                                .andExpect(jsonPath("$[1].fromValue").value("P1"))
                                .andExpect(jsonPath("$[1].toValue").value("P2"));
        }

        @Test
        void updateTitleEmitsTitleChangedEvent() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "Updated title"
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[1].eventType").value("TITLE_CHANGED"))
                                .andExpect(jsonPath("$[1].toValue").value("Updated title"));
        }

        @Test
        void updateAssigneeEmitsAssigneeChangedEvent() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "assignee": "bob"
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[1].eventType").value("ASSIGNEE_CHANGED"))
                                .andExpect(jsonPath("$[1].fromValue").value("triage"))
                                .andExpect(jsonPath("$[1].toValue").value("bob"));
        }

        @Test
        void clearAssigneeWithBlankStringEmitsAssigneeChangedEvent() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "assignee": ""
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.assignee").value(nullValue()));

                mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[1].eventType").value("ASSIGNEE_CHANGED"))
                                .andExpect(jsonPath("$[1].fromValue").value("triage"))
                                .andExpect(jsonPath("$[1].toValue").value(nullValue()));
        }

        @Test
        void clearAssigneeWithWhitespaceReturnsNullAssignee() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "assignee": "   "
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.assignee").value(nullValue()));
        }

        @Test
        void updateWithSameValueDoesNotEmitExtraEvent() throws Exception {
                Long ticketId = createTicketAndReturnId();

                mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "priority": "P1"
                                                }
                                                """))
                                .andExpect(status().isOk());

                mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void createTicketWithBlankAssigneeSetsNull() throws Exception {
                mockMvc.perform(post("/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "Blank assignee ticket",
                                                  "description": "Assignee is empty string.",
                                                  "priority": "P2",
                                                  "assignee": ""
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.assignee").value(nullValue()));
        }

        @Test
        void createTicketWithWhitespaceAssigneeSetsNull() throws Exception {
                mockMvc.perform(post("/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "title": "Whitespace assignee ticket",
                                                  "description": "Assignee is whitespace.",
                                                  "priority": "P3",
                                                  "assignee": "   "
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.assignee").value(nullValue()));
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
