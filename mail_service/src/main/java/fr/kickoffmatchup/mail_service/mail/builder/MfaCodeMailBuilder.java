package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.MfaCode;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MfaCodeMailBuilder extends AbstractMailBuilder<MfaCode> {

    public MfaCodeMailBuilder(MailRenderer renderer) {
        super(renderer, MfaCode.class, "mfa-code");
    }

    @Override
    protected Map<String, Object> variables(MfaCode message) {
        return Map.of(
                "code", message.code());
    }
}
