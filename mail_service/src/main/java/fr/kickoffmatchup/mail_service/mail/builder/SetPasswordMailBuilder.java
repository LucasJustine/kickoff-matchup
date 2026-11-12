package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailMessage.SetPassword;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class SetPasswordMailBuilder implements MailBuilder<SetPassword> {

    private final MailRenderer renderer;

    public SetPasswordMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<SetPassword> messageType() {
        return SetPassword.class;
    }

    @Override
    public MailContent build(SetPassword message, Locale locale) {
        String subject = renderer.message("set-password.subject", locale);
        String body = renderer.render("mail/set-password", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "link", renderer.link("/reset-password", message.token())));
        return MailContent.html(subject, body);
    }
}
