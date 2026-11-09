package fr.kickoffmatchup.mail_service.service;

import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailFactory;
import fr.kickoffmatchup.mail_service.mail.MailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/** N'envoie que : le contenu est construit par le {@link MailBuilder} du type, choisi par la {@link MailFactory}. */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender sender;
    private final MailFactory factory;
    private final String from;

    public MailService(JavaMailSender sender, MailFactory factory, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.factory = factory;
        this.from = from;
    }

    public void send(MailMessage message) {
        MailContent content = factory.create(message);

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(content.subject());
            helper.setText(content.body(), content.html());
            sender.send(mime);
        } catch (MessagingException e) {
            throw new IllegalStateException("Impossible de construire l'email " + message.type(), e);
        }

        log.info("Email {} envoyé", message.type());
    }
}
