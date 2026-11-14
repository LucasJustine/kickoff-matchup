package fr.kickoffmatchup.mail_service.config;

import fr.kickoffmatchup.mail_contract.MailQueues;
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

    @Bean
    DirectExchange mailExchange() {
        return new DirectExchange(MailQueues.EXCHANGE, true, false);
    }

    // ---------- Mails d'auth ----------
    @Bean
    Queue mailAuthQueue() {
        return queueWithDlq(MailQueues.AUTH_QUEUE, MailQueues.AUTH_DLQ);
    }

    @Bean
    Queue mailAuthDlq(@Value("${app.mail.dlq-ttl:7d}") Duration dlqTtl) {
        return QueueBuilder.durable(MailQueues.AUTH_DLQ).ttl((int) dlqTtl.toMillis()).build();
    }

    @Bean
    Binding mailAuthBinding() {
        return BindingBuilder.bind(mailAuthQueue()).to(mailExchange()).with(MailQueues.AUTH_ROUTING_KEY);
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