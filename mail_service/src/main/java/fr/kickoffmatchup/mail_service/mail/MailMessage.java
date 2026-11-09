package fr.kickoffmatchup.mail_service.mail;


public record MailMessage(MailType type, String to, String firstName, String token, String code, String locale) {
}
