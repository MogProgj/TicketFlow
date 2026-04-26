package com.mogproj.ticketflow;

import com.mogproj.ticketflow.tickets.TicketCommentRepository;
import com.mogproj.ticketflow.tickets.TicketEventRepository;
import com.mogproj.ticketflow.tickets.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

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
    void corsPreflightFromAllowedOriginReturns200() throws Exception {
        mockMvc.perform(options("/tickets")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsHeaderPresentOnGetRequest() throws Exception {
        mockMvc.perform(get("/tickets")
                .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void corsRequestFromDisallowedOriginIsRejected() throws Exception {
        mockMvc.perform(get("/tickets")
                .header("Origin", "http://evil.example.com"))
                .andExpect(status().isForbidden());
    }
}
