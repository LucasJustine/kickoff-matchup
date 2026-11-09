package fr.kickoffmatchup.auth_server.mail;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "mail.exchange";
    public static final String ROUTING_KEY = "mail.send";

    @Bean
    DirectExchange mailExchange() { return new DirectExchange(EXCHANGE, true, false); }

    @Bean
    MessageConverter jsonConverter() { return new JacksonJsonMessageConverter(); }
}
