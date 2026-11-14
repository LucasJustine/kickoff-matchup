package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.EmailChange;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailChangeMailBuilder extends AbstractMailBuilder<EmailChange> {

    public EmailChangeMailBuilder(MailRenderer renderer) {
        super(renderer, EmailChange.class, "email-change");
    }

    @Override
    protected Map<String, Object> variables(EmailChange message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/confirm-email-change", message.token()));
    }
}
