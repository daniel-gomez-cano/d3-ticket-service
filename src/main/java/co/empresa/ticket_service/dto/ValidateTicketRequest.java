package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body del POST /api/tickets/validate
 * El escáner de puerta envía el token leído del QR.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateTicketRequest {

    /** Token UUID extraído del QR escaneado */
    private String qrToken;
}
