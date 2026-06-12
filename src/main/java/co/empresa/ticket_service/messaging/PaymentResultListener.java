package co.empresa.ticket_service.messaging;

import co.empresa.ticket_service.config.OrderServiceClient;
import co.empresa.ticket_service.config.RabbitMQConfig;
import co.empresa.ticket_service.dto.CreateTicketRequest;
import co.empresa.ticket_service.dto.PaymentResultEvent;
import co.empresa.ticket_service.dto.TicketResponse;
import co.empresa.ticket_service.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Escucha PaymentResultEvent desde payment.result.ticket.queue.
 * Solo actúa cuando status=APPROVED.
 *
 * Por cada ítem del carrito y por cada unidad de cantidad,
 * genera un Ticket individual con QR único y envía el correo al comprador.
 *
 * Idempotencia: generateTicket() usa orderId como clave única.
 * orderId = cartId + "-" + ticketTypeId + "-" + índice
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultListener {

    private final TicketService ticketService;
    private final OrderServiceClient orderServiceClient;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULT_QUEUE)
    public void handlePaymentResult(PaymentResultEvent event) {
        if (!"APPROVED".equals(event.getStatus())) {
            log.debug("[RabbitMQ] Ignorando evento status={} para cartId={}",
                    event.getStatus(), event.getCartId());
            return;
        }

        log.info("[RabbitMQ] ← PaymentResultEvent APPROVED — cartId={} buyerId={}",
                event.getCartId(), event.getBuyerId());

        try {
            generateTicketsForCart(event);
        } catch (Exception e) {
            log.error("[RabbitMQ] Error generando tickets para cartId={}: {}",
                    event.getCartId(), e.getMessage(), e);
        }
    }

    private void generateTicketsForCart(PaymentResultEvent event) {
        OrderServiceClient.CartSummary cart =
                orderServiceClient.getCartSummary(event.getCartId());

        int totalGenerados = 0;

        for (OrderServiceClient.CartItem item : cart.items()) {
            for (int i = 0; i < item.quantity(); i++) {

                String orderId = event.getCartId() + "-" + item.ticketTypeId() + "-" + i;

                CreateTicketRequest req = new CreateTicketRequest(
                        item.ticketTypeId(),
                        item.ticketTypeName(),
                        item.eventId(),
                        orderId,
                        event.getBuyerId(),
                        event.getBuyerEmail()   // <-- se pasa al ticket-service para el correo
                );

                TicketResponse ticket = ticketService.generateTicket(req);

                log.info("[Tickets] Ticket generado — id={} orderId={} event={} buyer={}",
                        ticket.getId(), orderId, item.eventId(), event.getBuyerId());
                totalGenerados++;
            }
        }

        log.info("[Tickets] {} ticket(s) generados para cartId={}",
                totalGenerados, event.getCartId());
    }

}
