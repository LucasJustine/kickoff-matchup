package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_contract.MailMessage.AccountAlreadyExists;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class AccountAlreadyExistsMailBuilder implements MailBuilder<AccountAlreadyExists> {

    private final MailRenderer renderer;

    public AccountAlreadyExistsMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<AccountAlreadyExists> messageType() {
        return AccountAlreadyExists.class;
    }

    @Override
    public MailContent build(AccountAlreadyExists message, Locale locale) {
        String subject = renderer.message("account-already-exists.subject", locale);
        String body = renderer.render("mail/account-already-exists", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName())));
        return MailContent.html(subject, body);
    }
}
