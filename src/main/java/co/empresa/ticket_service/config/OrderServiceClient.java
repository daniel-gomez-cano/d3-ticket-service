package co.empresa.ticket_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Configuration
class OrderWebClientConfig {

    @Bean
    public WebClient orderServiceWebClient(
            @Value("${order.service.url:http://order-service:8083}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}

/**
 * Llama al endpoint interno del order-service para obtener los ítems
 * de un carrito pagado. No requiere JWT — es comunicación interna.
 *
 * Endpoint: GET /internal/carts/{cartId}/summary
 */
@Service
public class OrderServiceClient {

    private final WebClient webClient;

    public OrderServiceClient(WebClient orderServiceWebClient) {
        this.webClient = orderServiceWebClient;
    }

    public CartSummary getCartSummary(String cartId) {
        try {
            CartSummary summary = webClient.get()
                    .uri("/internal/carts/{cartId}/summary", cartId)
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            resp -> Mono.error(new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Carrito no encontrado en order-service: " + cartId)))
                    .bodyToMono(CartSummary.class)
                    .block();

            if (summary == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Resumen de carrito vacío para cartId=" + cartId);
            }
            return summary;

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener el carrito del order-service: " + e.getMessage());
        }
    }

    // ── DTOs del carrito (espejo de InternalCartSummaryResponse del order-service) ──

    public record CartSummary(
            String cartId,
            String buyerId,
            BigDecimal total,
            String currency,
            List<CartItem> items
    ) {}

    public record CartItem(
            String ticketTypeId,
            String ticketTypeName,
            String eventId,
            int quantity,
            BigDecimal unitPrice
    ) {}
}