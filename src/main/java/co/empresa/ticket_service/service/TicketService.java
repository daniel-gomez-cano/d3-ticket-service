package co.empresa.ticket_service.service;

import co.empresa.ticket_service.config.RabbitMQConfig;
import co.empresa.ticket_service.dto.CreateTicketRequest;
import co.empresa.ticket_service.dto.TicketNotificationEvent;
import co.empresa.ticket_service.dto.TicketResponse;
import co.empresa.ticket_service.dto.ValidationResult;
import co.empresa.ticket_service.model.Ticket;
import co.empresa.ticket_service.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepo;
    private final QrService qrService;
    private final RabbitTemplate rabbitTemplate;



    /** Lista todas las boletas del comprador autenticado */
    @Transactional(readOnly = true)
    public List<TicketResponse> getByBuyer(String buyerId) {
        return ticketRepo.findByBuyerId(buyerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    /**
     * Genera una boleta individual tras confirmación de pago.
     * Es idempotente: si ya existe boleta para ese orderId, la retorna sin crear otra.
     * Tras generar, publica un evento a RabbitMQ para que el notification-service
     * envíe el correo con el QR al comprador.
     */
    @Transactional
    public TicketResponse generateTicket(CreateTicketRequest req) {
        // Idempotencia: si ya existe, retornar la boleta existente sin reenviar correo
        Optional<Ticket> existing = ticketRepo.findByOrderId(req.getOrderId());
        if (existing.isPresent()) {
            log.info("Boleta ya existe para orderId={}, retornando existente", req.getOrderId());
            return toResponse(existing.get());
        }

        try {
            String qrToken = UUID.randomUUID().toString();
            String qrImageBase64 = qrService.generateQrBase64(qrToken);

            Ticket ticket = Ticket.builder()
                    .ticketTypeId(req.getTicketTypeId())
                    .ticketTypeName(req.getTicketTypeName())
                    .eventId(req.getEventId())
                    .orderId(req.getOrderId())
                    .buyerId(req.getBuyerId())
                    .qrToken(qrToken)
                    .qrImageBase64(qrImageBase64)
                    .status(Ticket.TicketStatus.ACTIVE)
                    .build();

            TicketResponse response = toResponse(ticketRepo.save(ticket));
            log.info("Boleta generada: id={} orderId={}", response.getId(), req.getOrderId());

            // Publicar evento al notification-service para enviar el correo con el QR
            publishNotificationEvent(req, response);

            return response;

        } catch (DataIntegrityViolationException e) {
            // Dos requests concurrentes con el mismo orderId — retornar la que ganó
            return ticketRepo.findByOrderId(req.getOrderId())
                    .map(this::toResponse)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Error generando la boleta"));
        }
    }

    /**
     * Publica un TicketNotificationEvent a RabbitMQ.
     * Si falla, solo se loguea — no debe interrumpir la generación de la boleta.
     */
    private void publishNotificationEvent(CreateTicketRequest req, TicketResponse ticket) {
        try {
            if (req.getBuyerEmail() == null || req.getBuyerEmail().isBlank()) {
                log.warn("buyerEmail vacío para orderId={}, no se enviará correo", req.getOrderId());
                return;
            }

            TicketNotificationEvent event = new TicketNotificationEvent(
                    req.getBuyerEmail(),
                    "Tu boleta para el evento - VivaEventos",
                    buildEmailBody(ticket),
                    ticket.getQrImageBase64()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATION_EXCHANGE,
                    RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                    event
            );

            log.info("Evento de notificación publicado para orderId={}", req.getOrderId());

        } catch (Exception e) {
            // La boleta ya fue creada — el correo fallido no debe revertir la transacción
            log.error("Error publicando notificación para orderId={}: {}", req.getOrderId(), e.getMessage());
        }
    }

    /**
     * Construye el HTML del correo con el QR embebido via cid:qr.
     * El notification-service adjuntará la imagen con ese mismo id.
     */
    private String buildEmailBody(TicketResponse ticket) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;">
                    <h2 style="color: #333;">¡Tu boleta está lista! 🎉</h2>
                    <p><b>Tipo de boleta:</b> %s</p>
                    <p><b>ID del evento:</b> %s</p>
                    <p><b>ID de orden:</b> %s</p>
                    <br/>
                    <p>Presenta este código QR en la puerta del evento:</p>
                    <img src="cid:qr" width="250" height="250" alt="QR Boleta"/>
                    <br/><br/>
                    <p style="color: #888; font-size: 12px;">
                        No compartas este QR. Es personal e intransferible.
                    </p>
                </div>
                """.formatted(
                ticket.getTicketTypeName(),
                ticket.getEventId(),
                ticket.getOrderId()
        );
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

        log.info("QR validado exitosamente: ticketId={} eventId={}", ticket.getId(), ticket.getEventId());

        return ValidationResult.builder()
                .valid(true)
                .message("Acceso permitido")
                .ticketId(ticket.getId())
                .ticketTypeName(ticket.getTicketTypeName())
                .eventId(ticket.getEventId())
                .buyerId(ticket.getBuyerId())
                .validatedAt(ticket.getUsedAt())
                .build();
    }

    /** El comprador consulta su boleta — solo puede ver las suyas */
    @Transactional(readOnly = true)
    public TicketResponse getById(String ticketId, String buyerId) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Boleta no encontrada"));

        if (!ticket.getBuyerId().equals(buyerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes acceso a esta boleta");

        return toResponse(ticket);
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .ticketTypeId(t.getTicketTypeId())
                .ticketTypeName(t.getTicketTypeName())
                .eventId(t.getEventId())
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
