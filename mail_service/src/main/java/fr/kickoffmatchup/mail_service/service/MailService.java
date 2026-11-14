package fr.kickoffmatchup.mail_service.service;

import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailFactory;
import fr.kickoffmatchup.mail_service.mail.MailMessage;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final int MAX_ADDRESS_LENGTH = 254;
    private static final String LOGO_CID = "logo";
    private static final ClassPathResource LOGO = new ClassPathResource("templates/assets/icon.png");

    private final JavaMailSender sender;
    private final MailFactory factory;
    private final String from;

    public MailService(JavaMailSender sender, MailFactory factory, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.factory = factory;
        this.from = from;
    }

    public void send(MailMessage message) {
        if (!isValidAddress(message.to())) {
            throw new AmqpRejectAndDontRequeueException(
                    "Destinataire invalide pour " + message.getClass().getSimpleName());
        }

        MailContent content = factory.create(message);

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(message.to());
            helper.setSubject(content.subject());
            helper.setText(content.body(), content.html());
            if (content.html()) {
                helper.addInline(LOGO_CID, LOGO, "image/png");
            }
            sender.send(mime);
        } catch (MessagingException e) {
            throw new IllegalStateException("Impossible de construire l'email " + message.getClass().getSimpleName(), e);
        }

        log.info("Email {} envoyé", message.getClass().getSimpleName());
    }

    private static boolean isValidAddress(String address) {
        if (address == null || address.isBlank() || address.length() > MAX_ADDRESS_LENGTH) {
            return false;
        }
        try {
            new InternetAddress(address, true);
            return true;
        } catch (AddressException e) {
            return false;
        }
    }
}
