package co.empresa.ticket_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.empresa.ticket_service.dto.CreateTicketTypeRequest;
import co.empresa.ticket_service.dto.TicketTypeResponse;
import co.empresa.ticket_service.dto.UpdateTicketTypeRequest;
import co.empresa.ticket_service.service.TicketTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * SCRUM-31: POST   /api/ticket-types
 * SCRUM-32: GET    /api/ticket-types/event/{eventId}
 * SCRUM-33: PUT    /api/ticket-types/{id}
 * SCRUM-34: DELETE /api/ticket-types/{id}
 */
@RestController
@RequestMapping("/api/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService service;

    // SCRUM-31
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ORGANIZER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TicketTypeResponse> create(
            @Valid @RequestBody CreateTicketTypeRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String organizerId = jwt.getSubject();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(req, organizerId));
    }

    // SCRUM-32
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketTypeResponse>> listByEvent(
            @PathVariable String eventId,
            @AuthenticationPrincipal Jwt jwt) {

        boolean isOrganizerOrAdmin = jwt != null &&
                jwt.getClaimAsStringList("roles") != null &&
                (jwt.getClaimAsStringList("roles").contains("ROLE_ORGANIZER") ||
                 jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN"));

        List<TicketTypeResponse> result = isOrganizerOrAdmin
                ? service.listAllByEvent(eventId)
                : service.listActiveByEvent(eventId);

        return ResponseEntity.ok(result);
    }

    // SCRUM-33
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ORGANIZER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TicketTypeResponse> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateTicketTypeRequest req,
            @AuthenticationPrincipal Jwt jwt) {

        String organizerId = jwt.getSubject();
        return ResponseEntity.ok(service.update(id, req, organizerId));
    }

    // SCRUM-34
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ORGANIZER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        service.delete(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
