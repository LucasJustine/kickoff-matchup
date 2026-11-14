package fr.kickoffmatchup.auth_server.mail;

import fr.kickoffmatchup.mail_contract.MailQueues;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    DirectExchange mailExchange() { return new DirectExchange(MailQueues.EXCHANGE, true, false); }

    @Bean
    MessageConverter jsonConverter() { return new JacksonJsonMessageConverter(); }
}
