package com.mogproj.ticketflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootEndpointReturnsApiInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.service").value("TicketFlow"))
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.health").value("/health"))
                .andExpect(jsonPath("$.tickets").value("/tickets"))
                .andExpect(jsonPath("$.swagger").value("/swagger-ui/index.html"));
    }
}
