package fr.kickoffmatchup.mail_service.mail.builder;

import fr.kickoffmatchup.mail_service.mail.MailBuilder;
import fr.kickoffmatchup.mail_service.mail.MailContent;
import fr.kickoffmatchup.mail_service.mail.MailMessage.EmailChangeNotice;
import fr.kickoffmatchup.mail_service.mail.MailRenderer;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class EmailChangeNoticeMailBuilder implements MailBuilder<EmailChangeNotice> {

    private final MailRenderer renderer;

    public EmailChangeNoticeMailBuilder(MailRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Class<EmailChangeNotice> messageType() {
        return EmailChangeNotice.class;
    }

    @Override
    public MailContent build(EmailChangeNotice message, Locale locale) {
        String subject = renderer.message("email-change-notice.subject", locale);
        String body = renderer.render("mail/email-change-notice", locale, Map.of(
                "subject", subject,
                "firstName", renderer.name(message.firstName()),
                "newEmail", message.newEmail() == null ? "" : message.newEmail()));
        return MailContent.html(subject, body);
    }
}
