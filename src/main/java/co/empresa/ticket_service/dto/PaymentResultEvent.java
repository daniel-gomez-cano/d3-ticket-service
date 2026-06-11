package co.empresa.ticket_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mensaje recibido desde el payment-service con el resultado de un pago.
 * Mismo DTO que usa order-service — los tres servicios deben ser idénticos.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentResultEvent {

    private String cartId;
    private String paymentId;
    private String mercadoPagoPaymentId;  // reutilizado: almacena el Stripe PaymentIntent ID
    private String buyerId;
    private String status;                // APPROVED | REJECTED | FAILED | REFUNDED
    private BigDecimal amount;
    private String statusDetail;
    private LocalDateTime processedAt;
}