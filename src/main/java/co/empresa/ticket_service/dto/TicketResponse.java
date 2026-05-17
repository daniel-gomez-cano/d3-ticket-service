package co.empresa.ticket_service.dto;

import co.empresa.ticket_service.model.Ticket.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Respuesta al generar o consultar una boleta individual.
 * Incluye el QR como imagen Base64 lista para mostrar en frontend:
 *   <img src="data:image/png;base64,{qrImageBase64}">
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {

    private String id;
    private String ticketTypeId;
    private String ticketTypeName;
    private String eventId;
    private String orderId;
    private String buyerId;

    /** UUID único codificado en el QR */
    private String qrToken;

    /** Imagen PNG del QR en Base64 */
    private String qrImageBase64;

    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime usedAt;
}
