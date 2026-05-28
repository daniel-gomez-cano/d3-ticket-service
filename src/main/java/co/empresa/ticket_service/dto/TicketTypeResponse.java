package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Respuesta de los endpoints de tipos de boleta.
 * GET /api/ticket-types/event/{eventId}
 * POST /api/ticket-types
 * PUT  /api/ticket-types/{id}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeResponse {

    private String id;
    private String eventId;
    private String name;
    private BigDecimal price;
    private int totalCapacity;
    private int remainingCapacity;
    private boolean active;
    private String organizerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
