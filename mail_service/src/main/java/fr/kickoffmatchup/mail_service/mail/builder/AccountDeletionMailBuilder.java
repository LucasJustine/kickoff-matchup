package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.AccountDeletion;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccountDeletionMailBuilder extends AbstractMailBuilder<AccountDeletion> {

    public AccountDeletionMailBuilder(MailRenderer renderer) {
        super(renderer, AccountDeletion.class, "account-deletion");
    }

    @Override
    protected Map<String, Object> variables(AccountDeletion message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/confirm-account-deletion", message.token()));
    }
}
