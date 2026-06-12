package co.empresa.ticket_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El ticket-service:
 *   - CONSUME de payment.result.ticket.queue (pagos aprobados)
 *   - PUBLICA a notification.exchange (para enviar correo con boleta)
 */
@Configuration
public class RabbitMQConfig {

    // ── Pago (consume) ───────────────────────────────────────────
    public static final String PAYMENT_EXCHANGE      = "payment.exchange";
    public static final String PAYMENT_RESULT_QUEUE  = "payment.result.ticket.queue";
    public static final String PAYMENT_RESULT_KEY    = "payment.result";

    // ── Notificación (publica) ───────────────────────────────────
    public static final String NOTIFICATION_EXCHANGE     = "notification.exchange";
    public static final String NOTIFICATION_QUEUE        = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY  = "notification.send";

    // ── Beans: payment ───────────────────────────────────────────

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

    // ── Beans: notification ──────────────────────────────────────

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    // ── Converter y template ─────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

}
