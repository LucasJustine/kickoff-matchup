package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_contract.MailMessage.VerifyEmail;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class VerifyEmailMailBuilder implements MailBuilder<VerifyEmail> {

    private final MailRenderer renderer;

    public VerifyEmailMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<VerifyEmail> messageType() {
        return VerifyEmail.class;
    }

    @Override
    public MailContent build(VerifyEmail message, Locale locale) {
        String subject = renderer.message("verify-email.subject", locale);
        String body = renderer.render("mail/verify-email", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/verify-email", message.token())));
        return MailContent.html(subject, body);
    }
}
