package fr.kickoffmatchup.mail_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "mail.exchange";
    public static final String ROUTING_KEY = "mail.send";
    public static final String QUEUE = "mail.queue";
    public static final String DLQ = "mail.queue.dlq";

    @Bean
    DirectExchange mailExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue mailQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }

    @Bean
    Queue mailDeadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding mailBinding(Queue mailQueue, DirectExchange mailExchange) {
        return BindingBuilder.bind(mailQueue).to(mailExchange).with(ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
