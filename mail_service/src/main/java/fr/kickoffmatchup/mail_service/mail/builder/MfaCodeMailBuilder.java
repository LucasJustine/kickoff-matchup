package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_contract.MailMessage.MfaCode;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class MfaCodeMailBuilder implements MailBuilder<MfaCode> {

    private final MailRenderer renderer;

    public MfaCodeMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<MfaCode> messageType() {
        return MfaCode.class;
    }

    @Override
    public MailContent build(MfaCode message, Locale locale) {
        String subject = renderer.message("mfa-code.subject", locale);
        String body = renderer.render("mail/mfa-code", locale, Map.of(
                "subject", subject,
                "code", message.code()));
        return MailContent.html(subject, body);
    }
}
