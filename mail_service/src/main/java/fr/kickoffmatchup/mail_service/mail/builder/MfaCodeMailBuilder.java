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
public class MfaCodeMailBuilder implements MailBuilder {

    private final MailRenderer renderer;

    public MfaCodeMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public MailType type() {
        return MailType.MFA_CODE;
    }

    @Override
    public MailContent build(MailMessage message, Locale locale) {
        String subject = renderer.message("mfa-code.subject", locale, message.code());   // {0} = code
        String body = renderer.render("mail/mfa-code", locale, Map.of(
                "subject", subject,
                "code", message.code()));
        return MailContent.html(subject, body);
    }
}
