package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.SetPassword;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SetPasswordMailBuilder extends AbstractMailBuilder<SetPassword> {

    public SetPasswordMailBuilder(MailRenderer renderer) {
        super(renderer, SetPassword.class, "set-password");
    }

    @Override
    protected Map<String, Object> variables(SetPassword message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/reset-password", message.token()));
    }
}
