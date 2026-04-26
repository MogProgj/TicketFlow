package com.mogproj.ticketflow.tickets;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicket_IdOrderByCreatedAtAscIdAsc(Long ticketId);
}
