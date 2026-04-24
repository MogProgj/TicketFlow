package com.mogproj.ticketflow.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "service", "TicketFlow",
                "status", "ok",
                "health", "/health",
                "tickets", "/tickets",
                "swagger", "/swagger-ui/index.html");
    }
}
