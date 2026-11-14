package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.AccountAlreadyExists;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AccountAlreadyExistsMailBuilder extends AbstractMailBuilder<AccountAlreadyExists> {

    public AccountAlreadyExistsMailBuilder(MailRenderer renderer) {
        super(renderer, AccountAlreadyExists.class, "account-already-exists");
    }

    @Override
    protected Map<String, Object> variables(AccountAlreadyExists message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()));
    }
}
