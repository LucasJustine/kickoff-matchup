package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailMessage.AccountDeletion;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class AccountDeletionMailBuilder implements MailBuilder<AccountDeletion> {

    private final MailRenderer renderer;

    public AccountDeletionMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<AccountDeletion> messageType() {
        return AccountDeletion.class;
    }

    @Override
    public MailContent build(AccountDeletion message, Locale locale) {
        String subject = renderer.message("account-deletion.subject", locale);
        String body = renderer.render("mail/account-deletion", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/confirm-account-deletion", message.token())));
        return MailContent.html(subject, body);
    }
}
