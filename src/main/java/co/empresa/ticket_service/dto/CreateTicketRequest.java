package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body del POST /api/tickets/generate
 * Lo llama el payment-service (o admin) tras confirmar un pago.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    /** ID del tipo de boleta (General, VIP, etc.) */
    private String ticketTypeId;

    /** ID de la orden en el payment-service — usado para idempotencia */
    private String orderId;

    /** sub de Keycloak del comprador */
    private String buyerId;
}
