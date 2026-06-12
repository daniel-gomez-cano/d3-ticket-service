package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body del POST /api/tickets/generate
 * Lo llama el payment-service (o admin) tras confirmar un pago.
 *
 * Se agrega buyerEmail para poder enviar el correo con la boleta
 * sin necesidad de llamar a Keycloak para resolver el sub → email.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    /** ID del ticketType en el event-service */
    private String ticketTypeId;

    /** Nombre del tipo (ej: "General", "VIP") — se guarda en la boleta */
    private String ticketTypeName;

    /** ID del evento en el event-service */
    private String eventId;

    /** ID de la orden en el payment-service — usado para idempotencia */
    private String orderId;

    /** sub de Keycloak del comprador */
    private String buyerId;

    /** Correo del comprador — necesario para enviar la boleta por email */
    private String buyerEmail;

}
