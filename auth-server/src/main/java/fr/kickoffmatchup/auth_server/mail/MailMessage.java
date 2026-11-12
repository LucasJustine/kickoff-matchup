package fr.kickoffmatchup.auth_server.mail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = MailMessage.VerifyEmail.class, name = "VERIFY_EMAIL"),
        @JsonSubTypes.Type(value = MailMessage.PasswordReset.class, name = "PASSWORD_RESET"),
        @JsonSubTypes.Type(value = MailMessage.MfaCode.class, name = "MFA_CODE"),
        @JsonSubTypes.Type(value = MailMessage.AccountAlreadyExists.class, name = "ACCOUNT_ALREADY_EXISTS"),
        @JsonSubTypes.Type(value = MailMessage.EmailChange.class, name = "EMAIL_CHANGE"),
        @JsonSubTypes.Type(value = MailMessage.EmailChangeNotice.class, name = "EMAIL_CHANGE_NOTICE"),
        @JsonSubTypes.Type(value = MailMessage.SetPassword.class, name = "SET_PASSWORD")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public sealed interface MailMessage {

    String to();

    String locale();

    record VerifyEmail(String to, String locale, String firstName, String token) implements MailMessage {}

    record PasswordReset(String to, String locale, String firstName, String token) implements MailMessage {}

    record MfaCode(String to, String locale, String code) implements MailMessage {}

    record AccountAlreadyExists(String to, String locale, String firstName) implements MailMessage {}

    record EmailChange(String to, String locale, String firstName, String token) implements MailMessage {}

    record EmailChangeNotice(String to, String locale, String firstName, String newEmail) implements MailMessage {}

    record SetPassword(String to, String locale, String firstName, String token) implements MailMessage {}
}
