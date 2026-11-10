package fr.kickoffmatchup.mail_service.mail;

import java.util.Locale;

public interface MailBuilder<T extends MailMessage> {

    Class<T> messageType();

    MailContent build(T message, Locale locale);
}
