package co.empresa.ticket_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Body del POST /api/ticket-types  (SCRUM-31)
 * Solo puede llamarlo un ROLE_ORGANIZER o ROLE_ADMIN.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketTypeRequest {

    @NotBlank(message = "El eventId es obligatorio")
    private String eventId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String name;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
    private BigDecimal price;

    @Min(value = 1, message = "La capacidad mínima es 1")
    private int totalCapacity;
}
