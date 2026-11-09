package fr.kickoffmatchup.mail_service.mail;

import fr.kickoffmatchup.mail_service.config.RabbitConfig;
import fr.kickoffmatchup.mail_service.service.MailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MailListener {

    private final MailService mailService;

    public MailListener(MailService mailService) {
        this.mailService = mailService;
    }

    
    @RabbitListener(queues = RabbitConfig.AUTH_QUEUE)
    public void on(MailMessage message) {
        mailService.send(message);
    }
}
