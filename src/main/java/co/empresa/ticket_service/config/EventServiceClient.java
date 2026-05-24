package co.empresa.ticket_service.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

/**
 * Cliente HTTP para comunicarse con el event-service.
 *
 * Dentro de la red Docker, la URL es: http://event-service:8081
 * En desarrollo local: http://localhost:8081
 *
 

/**
 * Llama al event-service para verificar que un evento existe
 * antes de crear un tipo de boleta.
 *
 * IMPORTANTE: No accede a la base de datos del event-service directamente.
 * Usa su API REST — así respetamos los límites entre microservicios.
 */
@Service
public class EventServiceClient {

    private final WebClient webClient;

    public EventServiceClient(WebClient eventServiceWebClient) {
        this.webClient = eventServiceWebClient;
    }

    public void verifyEventOwnership(String eventId, String organizerId) {
        try {
            EventResponse event = webClient.get()
                    .uri("/api/events/{id}", eventId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404,
                            resp -> Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "Evento no encontrado: " + eventId)))
                    .bodyToMono(EventResponse.class)
                    .block();

            if (event != null && !organizerId.equals(event.organizerId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No eres el organizador de este evento");
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo contactar el event-service");
        }
    }

    record EventResponse(String id, String name, String organizerId) {}
}