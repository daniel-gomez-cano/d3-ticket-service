package co.empresa.ticket_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Boleta individual generada tras confirmación de pago.
 * Cada boleta tiene un QR único e irrepetible.
 */
@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    /** ID de la orden en el payment-service (referencia externa) */
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    /** sub de Keycloak del comprador */
    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    /** Token UUID único que se codifica en el QR */
    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken;

    /** QR como imagen PNG en Base64 — listo para mostrar/enviar al cliente */
    @Column(name = "qr_image_base64", columnDefinition = "TEXT")
    private String qrImageBase64;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Se llena la primera vez que el QR es escaneado en puerta */
    private LocalDateTime usedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TicketStatus.ACTIVE;
    }

    public enum TicketStatus {
        ACTIVE,    // válida, nunca usada
        USED,      // ya ingresó al evento
        CANCELLED  // evento cancelado o reembolso procesado
    }
}
