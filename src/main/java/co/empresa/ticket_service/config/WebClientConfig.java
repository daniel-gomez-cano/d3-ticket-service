package co.empresa.ticket_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient eventServiceWebClient(
            @Value("${event.service.url:http://localhost:8081}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}