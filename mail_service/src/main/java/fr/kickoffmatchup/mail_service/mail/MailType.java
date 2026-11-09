package fr.kickoffmatchup.mail_service.mail;

public enum MailType {
    VERIFY_EMAIL,
    PASSWORD_RESET,
    MFA_CODE,
    ACCOUNT_ALREADY_EXISTS
}
