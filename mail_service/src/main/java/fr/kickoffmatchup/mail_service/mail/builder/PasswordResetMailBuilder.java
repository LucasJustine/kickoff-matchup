package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.PasswordReset;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PasswordResetMailBuilder extends AbstractMailBuilder<PasswordReset> {

    public PasswordResetMailBuilder(MailRenderer renderer) {
        super(renderer, PasswordReset.class, "password-reset");
    }

    @Override
    protected Map<String, Object> variables(PasswordReset message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/reset-password", message.token()));
    }
}
