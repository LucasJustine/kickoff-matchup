package fr.kickoffmatchup.mail_service.mail;


public record MailContent(String subject, String body, boolean html) {

    public static MailContent html(String subject, String body) {
        return new MailContent(subject, body, true);
    }

    public static MailContent text(String subject, String body) {
        return new MailContent(subject, body, false);
    }
}
