package fr.kickoffmatchup.mail_service.mail;

import fr.kickoffmatchup.mail_contract.MailMessage;
import fr.kickoffmatchup.mail_contract.MailQueues;
import fr.kickoffmatchup.mail_service.service.MailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MailListener {

    private final MailService mailService;

    public MailListener(MailService mailService) {
        this.mailService = mailService;
    }

    
    @RabbitListener(queues = MailQueues.AUTH_QUEUE)
    public void on(MailMessage message) {
        mailService.send(message);
    }
}
