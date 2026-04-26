package com.mogproj.ticketflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mogproj.ticketflow.tickets.TicketCommentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private TicketEventRepository ticketEventRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketEventRepository.deleteAllInBatch();
        ticketCommentRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
    }

    @Test
    void createCommentReturns201WithCommentFields() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "author": "alice",
                          "body": "Looking into this now."
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.body").value("Looking into this now."))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createCommentWithBlankAuthorReturns400() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "author": "",
                          "body": "Some body."
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.author").exists());
    }

    @Test
    void createCommentWithBlankBodyReturns400() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "author": "alice",
                          "body": "   "
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.body").exists());
    }

    @Test
    void createCommentOnMissingTicketReturns404() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", 9999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "author": "alice",
                          "body": "A comment."
                        }
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 9999 was not found."));
    }

    @Test
    void listCommentsReturnsCommentsInChronologicalOrder() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "author": "alice", "body": "First comment." }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "author": "bob", "body": "Second comment." }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/tickets/{ticketId}/comments", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].author").value("alice"))
                .andExpect(jsonPath("$[1].author").value("bob"));
    }

    @Test
    void listCommentsOnMissingTicketReturns404() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ticket 9999 was not found."));
    }

    @Test
    void createCommentEmitsCommentAddedEvent() throws Exception {
        Long ticketId = createTicketAndReturnId();

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "author": "alice",
                          "body": "Investigating now."
                        }
                        """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/tickets/{ticketId}/events", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].eventType").value("COMMENT_ADDED"))
                .andExpect(jsonPath("$[1].actor").value("alice"))
                .andExpect(jsonPath("$[1].toValue").value(org.hamcrest.Matchers.startsWith("comment:")));
    }

    private Long createTicketAndReturnId() throws Exception {
        String responseBody = mockMvc.perform(post("/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Bug in payment flow",
                          "description": "Payments are failing intermittently.",
                          "priority": "P1",
                          "assignee": "triage"
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody).path("id").asLong();
    }
}
