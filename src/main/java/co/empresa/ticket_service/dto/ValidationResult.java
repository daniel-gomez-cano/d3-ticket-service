package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Respuesta del endpoint POST /api/tickets/validate
 * Siempre retorna HTTP 200 — el campo "valid" indica si se permite el acceso.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {

    /** true = acceso permitido, false = rechazado */
    private boolean valid;

    /** Mensaje legible para mostrar en el escáner de puerta */
    private String message;

    private String ticketId;
    private String ticketTypeName;
    private String eventId;
    private String buyerId;
    private LocalDateTime validatedAt;
}
