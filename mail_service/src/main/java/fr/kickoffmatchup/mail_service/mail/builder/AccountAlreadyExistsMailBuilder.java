package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailMessage;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import fr.kickoffmatchup.mail_service.mail.MailType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class AccountAlreadyExistsMailBuilder implements MailBuilder {

    private final MailRenderer renderer;

    public AccountAlreadyExistsMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public MailType type() {
        return MailType.ACCOUNT_ALREADY_EXISTS;
    }

    @Override
    public MailContent build(MailMessage message, Locale locale) {
        String subject = renderer.message("account-already-exists.subject", locale);
        String body = renderer.render("mail/account-already-exists", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message)));
        return MailContent.html(subject, body);
    }
}
