package co.empresa.ticket_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El ticket-service solo CONSUME de payment.result.queue.
 * No publica eventos — solo escucha pagos aprobados para generar tickets.
 *
 * Declara la misma topología que order-service y payment-service:
 * RabbitMQ es idempotente al re-declarar exchanges/colas con la misma config.
 */
@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE     = "payment.exchange";
    public static final String PAYMENT_RESULT_QUEUE = "payment.result.queue";
    public static final String PAYMENT_RESULT_KEY   = "payment.result";

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentResultQueue() {
        return QueueBuilder.durable(PAYMENT_RESULT_QUEUE).build();
    }

    @Bean
    public Binding paymentResultBinding() {
        return BindingBuilder
                .bind(paymentResultQueue())
                .to(paymentExchange())
                .with(PAYMENT_RESULT_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}