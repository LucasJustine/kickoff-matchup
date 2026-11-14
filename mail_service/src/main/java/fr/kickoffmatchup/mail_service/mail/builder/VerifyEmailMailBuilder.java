package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.VerifyEmail;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class VerifyEmailMailBuilder extends AbstractMailBuilder<VerifyEmail> {

    public VerifyEmailMailBuilder(MailRenderer renderer) {
        super(renderer, VerifyEmail.class, "verify-email");
    }

    @Override
    protected Map<String, Object> variables(VerifyEmail message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/verify-email", message.token()));
    }
}
