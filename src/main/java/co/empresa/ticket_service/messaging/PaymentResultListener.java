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
 * Escucha PaymentResultEvent desde payment.result.queue.
 * Solo actúa cuando status=APPROVED — ignora el resto.
 *
 * Por cada ítem del carrito y por cada unidad de cantidad,
 * genera un Ticket individual con QR único.
 *
 * Idempotencia: generateTicket() usa orderId como clave única.
 * orderId = cartId + "-" + ticketTypeId + "-" + índice
 * Si el listener recibe el mismo evento dos veces, los tickets
 * ya existen y se devuelven sin crear duplicados.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentResultListener {

    private final TicketService      ticketService;
    private final OrderServiceClient orderServiceClient;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_RESULT_QUEUE)
    public void handlePaymentResult(PaymentResultEvent event) {

        // Solo procesar pagos aprobados
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
            // No relanzamos — si RabbitMQ reintenta, la idempotencia del generateTicket
            // garantiza que no se crean duplicados en el segundo intento.
            log.error("[RabbitMQ] Error generando tickets para cartId={}: {}",
                    event.getCartId(), e.getMessage(), e);
        }
    }

    private void generateTicketsForCart(PaymentResultEvent event) {
        // Obtener ítems del carrito desde order-service (endpoint interno sin auth)
        OrderServiceClient.CartSummary cart =
                orderServiceClient.getCartSummary(event.getCartId());

        int totalGenerados = 0;

        for (OrderServiceClient.CartItem item : cart.items()) {
            // Generar un ticket por cada unidad comprada
            // Ej: si quantity=2 de General, se generan 2 tickets individuales con QR distinto
            for (int i = 0; i < item.quantity(); i++) {

                // orderId único por boleta: cartId-ticketTypeId-índice
                // Garantiza idempotencia: si se reprocesa el evento, se retorna el ticket existente
                String orderId = event.getCartId() + "-" + item.ticketTypeId() + "-" + i;

                CreateTicketRequest req = new CreateTicketRequest(
                        item.ticketTypeId(),
                        item.ticketTypeName(),
                        item.eventId(),
                        orderId,
                        event.getBuyerId()
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