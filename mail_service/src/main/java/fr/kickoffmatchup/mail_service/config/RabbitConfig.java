package fr.kickoffmatchup.mail_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "mail.exchange";

    public static final String AUTH_ROUTING_KEY = "mail.auth";
    public static final String AUTH_QUEUE = "mail.auth.queue";
    public static final String AUTH_DLQ = "mail.auth.queue.dlq";
    
    @Bean
    DirectExchange mailExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    // ---------- Mails d'auth ----------
    @Bean
    Queue mailAuthQueue() {
        return queueWithDlq(AUTH_QUEUE, AUTH_DLQ);
    }

    @Bean
    Queue mailAuthDlq(@Value("${app.mail.dlq-ttl:7d}") Duration dlqTtl) {
        return QueueBuilder.durable(AUTH_DLQ).ttl((int) dlqTtl.toMillis()).build();
    }

    @Bean
    Binding mailAuthBinding() {
        return BindingBuilder.bind(mailAuthQueue()).to(mailExchange()).with(AUTH_ROUTING_KEY);
    }

   
    @Bean
    MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }


    private Queue queueWithDlq(String queue, String dlq) {
        return QueueBuilder.durable(queue)
                .deadLetterExchange("")
                .deadLetterRoutingKey(dlq)
                .build();
    }
}