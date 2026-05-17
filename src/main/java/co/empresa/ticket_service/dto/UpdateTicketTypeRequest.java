package co.empresa.ticket_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Body del PUT /api/ticket-types/{id}  (SCRUM-33)
 * Todos los campos son opcionales — solo se actualiza lo que venga.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketTypeRequest {

    @Size(max = 100)
    private String name;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @Min(value = 1)
    private Integer totalCapacity;

    /** null = no cambiar, true/false = activar/desactivar ventas */
    private Boolean active;
}
