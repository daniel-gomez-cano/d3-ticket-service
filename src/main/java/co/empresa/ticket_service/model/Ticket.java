package co.empresa.ticket_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Boleta individual generada tras confirmación de pago.
 * Cada boleta tiene un QR único e irrepetible.
 *
 * Los tipos de boleta (precio, stock, cantidad) son responsabilidad del
 * event-service. Aquí solo guardamos referencias externas para no acoplar
 * los dos servicios a nivel de base de datos.
 */
@Entity
@Table(name = "tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * ID del ticketType en el event-service (referencia externa).
     * Ej: el ID 3 o 4 que devuelve POST /api/v1/events con sus ticketTypes.
     */
    @Column(name = "ticket_type_id", nullable = false)
    private String ticketTypeId;

    /**
     * Nombre del tipo de boleta guardado localmente para no llamar
     * al event-service cada vez que se muestra la boleta.
     * Ej: "General", "VIP"
     */
    @Column(name = "ticket_type_name", nullable = false)
    private String ticketTypeName;

    /**
     * ID del evento en el event-service (referencia externa).
     * Necesario para validación en puerta y trazabilidad.
     */
    @Column(name = "event_id", nullable = false)
    private String eventId;

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
