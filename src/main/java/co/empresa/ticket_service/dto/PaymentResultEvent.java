package co.empresa.ticket_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje recibido desde el payment-service con el resultado de un pago.
 * Mismo DTO que usa order-service — los tres servicios deben ser idénticos.
 *
 * Se agrega buyerEmail para poder enviar el correo con la boleta
 * sin necesidad de llamar a Keycloak.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResultEvent {

    private String cartId;
    private String paymentId;
    private String mercadoPagoPaymentId;
    private String buyerId;
    private String buyerEmail;        // nuevo — el payment-service debe incluirlo
    private String status;            // APPROVED | REJECTED | FAILED | REFUNDED
    private BigDecimal amount;
    private String statusDetail;
    private LocalDateTime processedAt;

}
