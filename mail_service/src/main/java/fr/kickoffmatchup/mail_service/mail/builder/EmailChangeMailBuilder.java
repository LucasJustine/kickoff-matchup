package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_contract.MailMessage.EmailChange;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class EmailChangeMailBuilder implements MailBuilder<EmailChange> {

    private final MailRenderer renderer;

    public EmailChangeMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<EmailChange> messageType() {
        return EmailChange.class;
    }

    @Override
    public MailContent build(EmailChange message, Locale locale) {
        String subject = renderer.message("email-change.subject", locale);
        String body = renderer.render("mail/email-change", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/confirm-email-change", message.token())));
        return MailContent.html(subject, body);
    }
}
