package co.empresa.ticket_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Evento que el ticket-service publica a RabbitMQ
 * para que el notification-service envíe el correo con la boleta.
 *
 * Debe tener la misma estructura que NotificationEvent en el notification-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketNotificationEvent {

    private String recipient;       // email del comprador
    private String subject;
    private String body;            // HTML con <img src="cid:qr">
    private String qrImageBase64;   // imagen PNG del QR en base64

}
