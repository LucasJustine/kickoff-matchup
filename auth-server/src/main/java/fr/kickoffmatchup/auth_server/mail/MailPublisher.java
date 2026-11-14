package fr.kickoffmatchup.auth_server.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import fr.kickoffmatchup.auth_server.mail.EmailService.MailRequested;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailPublisher {

    private static final Logger log = LoggerFactory.getLogger(MailPublisher.class);

    private final RabbitTemplate rabbit;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MailRequested event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event.message());
        } catch (AmqpException e) {
            log.error("Mail {} non publié (RabbitMQ indisponible) : l'utilisateur ne le recevra pas",
                    event.message().getClass().getSimpleName(), e);
        }
    }
}
