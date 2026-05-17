package co.empresa.ticket_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente HTTP para comunicarse con el event-service.
 *
 * Dentro de la red Docker, la URL es: http://event-service:8081
 * En desarrollo local: http://localhost:8081
 *
 * Se configura via variable de entorno EVENT_SERVICE_URL (ver application.properties).
 */
@Configuration
class WebClientConfig {

    @Bean
    public WebClient eventServiceWebClient(@Value("${event.service.url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}

/**
 * Llama al event-service para verificar que un evento existe
 * antes de crear un tipo de boleta.
 *
 * IMPORTANTE: No accede a la base de datos del event-service directamente.
 * Usa su API REST — así respetamos los límites entre microservicios.
 */
@Service
class EventServiceClient {

    private final WebClient webClient;

    EventServiceClient(WebClient eventServiceWebClient) {
        this.webClient = eventServiceWebClient;
    }

    /**
     * Verifica que el evento existe y que el organizador es su dueño.
     * Lanza 404 si el evento no existe, 403 si el organizador no coincide.
     *
     * Ajusta el path "/api/events/{id}" según lo que exponga el event-service real.
     */
    public void verifyEventOwnership(String eventId, String organizerId) {
        try {
            EventResponse event = webClient.get()
                    .uri("/api/events/{id}", eventId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                        resp -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Evento no encontrado: " + eventId); })
                    .bodyToMono(EventResponse.class)
                    .block(); // bloqueante — OK para MVP; usar reactive en producción

            if (event != null && !organizerId.equals(event.organizerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No eres el organizador de este evento");
            }

        } catch (ResponseStatusException e) {
            throw e; // re-lanzar las nuestras
        } catch (Exception e) {
            // Si el event-service no responde, lanzar 503 para que el cliente sepa
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo contactar el event-service. Intenta de nuevo.");
        }
    }

    /** DTO mínimo de la respuesta del event-service */
    record EventResponse(String id, String name, String organizerId) {}
}
