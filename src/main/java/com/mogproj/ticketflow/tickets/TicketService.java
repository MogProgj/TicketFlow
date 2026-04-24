package com.mogproj.ticketflow.tickets;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mogproj.ticketflow.tickets.dto.CreateTicketRequest;
import com.mogproj.ticketflow.tickets.dto.TicketEventResponse;
import com.mogproj.ticketflow.tickets.dto.TicketResponse;
import com.mogproj.ticketflow.tickets.dto.UpdateTicketRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TicketService {

    private static final String SYSTEM_ACTOR = "system";

    private static final Map<Ticket.Status, Set<Ticket.Status>> ALLOWED_TRANSITIONS = allowedTransitions();

    private final TicketRepository ticketRepository;
    private final TicketEventRepository ticketEventRepository;

    public TicketService(TicketRepository ticketRepository, TicketEventRepository ticketEventRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketEventRepository = ticketEventRepository;
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(normalizeRequired(request.getTitle()));
        ticket.setDescription(normalizeRequired(request.getDescription()));
        ticket.setPriority(request.getPriority());
        ticket.setAssignee(normalizeOptional(request.getAssignee()));
        ticket.setStatus(Ticket.Status.OPEN);

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);
        createEvent(savedTicket, TicketEvent.EventType.CREATED, null, Ticket.Status.OPEN.name());

        return toTicketResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(Ticket.Status status, Ticket.Priority priority, String query) {
        Specification<Ticket> specification = buildSpecification(status, priority, query);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt", "id");
        return ticketRepository.findAll(specification, sort).stream()
                .map(this::toTicketResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long ticketId) {
        return toTicketResponse(getTicketEntity(ticketId));
    }

    @Transactional
    public TicketResponse updateTicket(Long ticketId, UpdateTicketRequest request) {
        Ticket ticket = getTicketEntity(ticketId);
        Ticket.Status previousStatus = ticket.getStatus();
        boolean statusChanged = request.getStatus() != null && request.getStatus() != previousStatus;

        if (statusChanged) {
            validateStatusTransition(previousStatus, request.getStatus());
        }

        if (request.getTitle() != null) {
            ticket.setTitle(normalizeRequired(request.getTitle()));
        }
        if (request.getDescription() != null) {
            ticket.setDescription(normalizeRequired(request.getDescription()));
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getAssignee() != null) {
            ticket.setAssignee(normalizeOptional(request.getAssignee()));
        }
        if (statusChanged) {
            ticket.setStatus(request.getStatus());
        }

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);

        if (statusChanged) {
            createEvent(savedTicket, TicketEvent.EventType.STATUS_CHANGED, previousStatus.name(),
                    savedTicket.getStatus().name());
        }

        return toTicketResponse(savedTicket);
    }

    @Transactional(readOnly = true)
    public List<TicketEventResponse> getTicketEvents(Long ticketId) {
        getTicketEntity(ticketId);
        return ticketEventRepository.findByTicket_IdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .map(this::toTicketEventResponse)
                .toList();
    }

    private Ticket getTicketEntity(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private void validateStatusTransition(Ticket.Status currentStatus, Ticket.Status nextStatus) {
        if (currentStatus == nextStatus) {
            return;
        }

        Set<Ticket.Status> allowedStatuses = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedStatuses.contains(nextStatus)) {
            throw new InvalidTicketStatusTransitionException(currentStatus, nextStatus);
        }
    }

    private void createEvent(Ticket ticket, TicketEvent.EventType eventType, String fromValue, String toValue) {
        TicketEvent event = new TicketEvent();
        event.setTicket(ticket);
        event.setActor(SYSTEM_ACTOR);
        event.setEventType(eventType);
        event.setFromValue(fromValue);
        event.setToValue(toValue);
        ticketEventRepository.save(event);
    }

    private Specification<Ticket> buildSpecification(Ticket.Status status, Ticket.Priority priority, String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }
            if (StringUtils.hasText(query)) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern)));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private TicketResponse toTicketResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }

    private TicketEventResponse toTicketEventResponse(TicketEvent event) {
        return new TicketEventResponse(
                event.getId(),
                event.getEventType(),
                event.getActor(),
                event.getFromValue(),
                event.getToValue(),
                event.getCreatedAt());
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static Map<Ticket.Status, Set<Ticket.Status>> allowedTransitions() {
        Map<Ticket.Status, Set<Ticket.Status>> transitions = new EnumMap<>(Ticket.Status.class);
        transitions.put(Ticket.Status.OPEN, EnumSet.of(Ticket.Status.IN_PROGRESS));
        transitions.put(Ticket.Status.IN_PROGRESS, EnumSet.of(Ticket.Status.WAITING, Ticket.Status.RESOLVED));
        transitions.put(Ticket.Status.WAITING, EnumSet.of(Ticket.Status.IN_PROGRESS, Ticket.Status.RESOLVED));
        transitions.put(Ticket.Status.RESOLVED, EnumSet.of(Ticket.Status.CLOSED));
        transitions.put(Ticket.Status.CLOSED, EnumSet.noneOf(Ticket.Status.class));
        return Map.copyOf(transitions);
    }
}
