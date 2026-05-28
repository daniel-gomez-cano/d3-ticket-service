package co.empresa.ticket_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plantilla de boleta dentro de un evento.
 * Ej: "General $50.000 – 200 cupos", "VIP $150.000 – 50 cupos"
 * No es la boleta en sí, sino el tipo que define precio y capacidad.
 */
@Entity
@Table(name = "ticket_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** ID del evento en el event-service (referencia externa, sin FK real entre servicios) */
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String name;            // "General", "VIP", "Estudiante"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int totalCapacity;      // cupos totales definidos por el organizador

    @Column(nullable = false)
    private int remainingCapacity;  // se decrementa al vender; nunca baja de 0

    @Column(nullable = false)
    private boolean active = true;  // el organizador puede pausar ventas

    /** sub de Keycloak del organizador dueño de este tipo de boleta */
    @Column(name = "organizer_id", nullable = false)
    private String organizerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        remainingCapacity = totalCapacity;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
