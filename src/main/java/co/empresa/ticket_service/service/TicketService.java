package co.empresa.ticket_service.service;

import co.empresa.ticket_service.dto.CreateTicketRequest;
import co.empresa.ticket_service.dto.TicketResponse;
import co.empresa.ticket_service.dto.ValidationResult;
import co.empresa.ticket_service.model.Ticket;
import co.empresa.ticket_service.model.TicketType;
import co.empresa.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketTypeService ticketTypeService;
    private final QrService qrService;

    /**
     * Genera una boleta individual tras confirmación de pago.
     * Es idempotente: si ya existe boleta para ese orderId, la retorna sin crear otra.
     */
    @Transactional
    public TicketResponse generateTicket(CreateTicketRequest req) {
        // Idempotencia: mismo orderId → misma boleta
        if (ticketRepo.existsByOrderId(req.getOrderId())) {
            return ticketRepo.findByOrderId(req.getOrderId())
                    .map(this::toResponse)
                    .orElseThrow();
        }

        // Reservar cupo (decrementa remainingCapacity atómicamente)
        TicketType ticketType = ticketTypeService.reserveOne(req.getTicketTypeId());

        String qrToken = UUID.randomUUID().toString();
        String qrImageBase64 = qrService.generateQrBase64(qrToken);

        Ticket ticket = Ticket.builder()
                .ticketType(ticketType)
                .orderId(req.getOrderId())
                .buyerId(req.getBuyerId())
                .qrToken(qrToken)
                .qrImageBase64(qrImageBase64)
                .status(Ticket.TicketStatus.ACTIVE)
                .build();

        return toResponse(ticketRepo.save(ticket));
    }

    /**
     * Valida un QR en la puerta del evento.
     * Si la boleta es ACTIVE → la marca USED y retorna valid=true.
     * Si ya es USED o no existe → retorna valid=false.
     */
    @Transactional
    public ValidationResult validateTicket(String qrToken) {
        Ticket ticket = ticketRepo.findByQrToken(qrToken).orElse(null);

        if (ticket == null) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("QR no reconocido")
                    .validatedAt(LocalDateTime.now())
                    .build();
        }

        if (ticket.getStatus() == Ticket.TicketStatus.USED) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("Boleta ya utilizada el " + ticket.getUsedAt())
                    .ticketId(ticket.getId())
                    .validatedAt(LocalDateTime.now())
                    .build();
        }

        if (ticket.getStatus() == Ticket.TicketStatus.CANCELLED) {
            return ValidationResult.builder()
                    .valid(false)
                    .message("Boleta cancelada")
                    .ticketId(ticket.getId())
                    .validatedAt(LocalDateTime.now())
                    .build();
        }

        ticket.setStatus(Ticket.TicketStatus.USED);
        ticket.setUsedAt(LocalDateTime.now());
        ticketRepo.save(ticket);

        return ValidationResult.builder()
                .valid(true)
                .message("Acceso permitido")
                .ticketId(ticket.getId())
                .ticketTypeName(ticket.getTicketType().getName())
                .eventId(ticket.getTicketType().getEventId())
                .buyerId(ticket.getBuyerId())
                .validatedAt(ticket.getUsedAt())
                .build();
    }

    /** El comprador consulta su boleta — solo puede ver las suyas */
    public TicketResponse getById(String ticketId, String buyerId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Boleta no encontrada"));

        if (!ticket.getBuyerId().equals(buyerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta boleta");

        return toResponse(ticket);
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketTypeId(t.getTicketType().getId())
                .ticketTypeName(t.getTicketType().getName())
                .eventId(t.getTicketType().getEventId())
                .orderId(t.getOrderId())
                .buyerId(t.getBuyerId())
                .qrToken(t.getQrToken())
                .qrImageBase64(t.getQrImageBase64())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .usedAt(t.getUsedAt())
                .build();
    }
}
