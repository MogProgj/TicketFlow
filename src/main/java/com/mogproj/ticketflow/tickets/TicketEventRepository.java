package com.mogproj.ticketflow.tickets;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    List<TicketEvent> findByTicket_IdOrderByCreatedAtAscIdAsc(Long ticketId);
}
