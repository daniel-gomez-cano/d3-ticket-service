package co.empresa.ticket_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.empresa.ticket_service.dto.CreateTicketRequest;
import co.empresa.ticket_service.dto.TicketResponse;
import co.empresa.ticket_service.dto.ValidateTicketRequest;
import co.empresa.ticket_service.dto.ValidationResult;
import co.empresa.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService service;

    /**
     * Genera una boleta con QR tras confirmación de pago.
     * En producción lo llama el payment-service; en el MVP lo llama un ADMIN.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TicketResponse> generate(
            @RequestBody CreateTicketRequest req) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.generateTicket(req));
    }

    /**
     * Valida QR en la puerta del evento.
     * Siempre retorna HTTP 200 — el campo "valid" indica si se permite el acceso.
     */
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ORGANIZER')")
    public ResponseEntity<ValidationResult> validate(
            @RequestBody ValidateTicketRequest req) {

        return ResponseEntity.ok(service.validateTicket(req.getQrToken()));
    }

    /**
     * El comprador consulta su propia boleta y ve la imagen QR.
     */
    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAuthority('ROLE_CLIENT') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable String ticketId,
            @AuthenticationPrincipal Jwt jwt) {

        String buyerId = jwt.getSubject();
        return ResponseEntity.ok(service.getById(ticketId, buyerId));
    }


    /**
     * El comprador lista todas sus propias boletas.
     */
    @GetMapping("/buyer/me")
    @PreAuthorize("hasAuthority('ROLE_CLIENT') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal Jwt jwt) {

        String buyerId = jwt.getSubject();
        return ResponseEntity.ok(service.getByBuyer(buyerId));
    }
}
