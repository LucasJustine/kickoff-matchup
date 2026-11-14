package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_contract.MailMessage.PasswordReset;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class PasswordResetMailBuilder implements MailBuilder<PasswordReset> {

    private final MailRenderer renderer;

    public PasswordResetMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<PasswordReset> messageType() {
        return PasswordReset.class;
    }

    @Override
    public MailContent build(PasswordReset message, Locale locale) {
        String subject = renderer.message("password-reset.subject", locale);
        String body = renderer.render("mail/password-reset", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/reset-password", message.token())));
        return MailContent.html(subject, body);
    }
}
