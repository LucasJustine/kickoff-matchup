package fr.kickoffmatchup.auth_server.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class EmailService {


    public enum MailType { VERIFY_EMAIL, PASSWORD_RESET, MFA_CODE, ACCOUNT_ALREADY_EXISTS }

    public record MailMessage(MailType type, String to, String firstName, String token, String code, String locale) {}

    public record MailRequested(MailMessage message) {}

    private final ApplicationEventPublisher events;

    public void sendVerification(String to, String firstName, String token) {
        events.publishEvent(new MailRequested(
                new MailMessage(MailType.VERIFY_EMAIL, to, firstName, token, null, locale())));
    }

    public void sendPasswordReset(String to, String firstName, String token) {
        events.publishEvent(new MailRequested(
                new MailMessage(MailType.PASSWORD_RESET, to, firstName, token, null, locale())));
    }

    public void sendMfaCode(String to, String code) {
        events.publishEvent(new MailRequested(
                new MailMessage(MailType.MFA_CODE, to, null, null, code, locale())));
    }

    public void sendAccountAlreadyExists(String to, String firstName) {
        events.publishEvent(new MailRequested(
                new MailMessage(MailType.ACCOUNT_ALREADY_EXISTS, to, firstName, null, null, locale())));
    }

    private static String locale() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getHeader(HttpHeaders.ACCEPT_LANGUAGE) != null) {
            return attributes.getRequest().getLocale().toLanguageTag();
        }
        return null;
    }
}
