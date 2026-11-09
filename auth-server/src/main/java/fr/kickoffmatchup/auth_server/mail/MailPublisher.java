package fr.kickoffmatchup.auth_server.mail;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import fr.kickoffmatchup.auth_server.mail.EmailService.MailRequested;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailPublisher {

    private final RabbitTemplate rabbit;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(MailRequested event) {
        rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, event.message());
    }
}
