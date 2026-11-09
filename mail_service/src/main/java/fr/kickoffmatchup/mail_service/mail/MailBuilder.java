package fr.kickoffmatchup.mail_service.mail;

import java.util.Locale;


public interface MailBuilder {

    MailType type();

    MailContent build(MailMessage message, Locale locale);
}
