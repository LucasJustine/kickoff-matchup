package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_contract.MailMessage.EmailChangeNotice;
import fr.kickoffmatchup.mail_service.mail.AbstractMailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailChangeNoticeMailBuilder extends AbstractMailBuilder<EmailChangeNotice> {

    public EmailChangeNoticeMailBuilder(MailRenderer renderer) {
        super(renderer, EmailChangeNotice.class, "email-change-notice");
    }

    @Override
    protected Map<String, Object> variables(EmailChangeNotice message) {
        return Map.of(
                "firstName", renderer.name(message.firstName()),
                "newEmail", message.newEmail() == null ? "" : message.newEmail());
    }
}
