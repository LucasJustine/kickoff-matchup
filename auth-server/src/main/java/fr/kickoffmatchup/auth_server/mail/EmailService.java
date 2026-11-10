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


    public record MailRequested(MailMessage message) {}

    private final ApplicationEventPublisher events;

    public void sendVerification(String to, String firstName, String token) {
        events.publishEvent(new MailRequested(
                new MailMessage.VerifyEmail(to, locale(), firstName, token)));
    }

    public void sendPasswordReset(String to, String firstName, String token) {
        events.publishEvent(new MailRequested(
                new MailMessage.PasswordReset(to, locale(), firstName, token)));
    }

    public void sendMfaCode(String to, String code) {
        events.publishEvent(new MailRequested(
                new MailMessage.MfaCode(to, locale(), code)));
    }

    public void sendAccountAlreadyExists(String to, String firstName) {
        events.publishEvent(new MailRequested(
                new MailMessage.AccountAlreadyExists(to, locale(), firstName)));
    }

    public void sendEmailChange(String to, String firstName, String token) {
        events.publishEvent(new MailRequested(
                new MailMessage.EmailChange(to, locale(), firstName, token)));
    }

    public void sendEmailChangeNotice(String to, String firstName, String newEmail) {
        events.publishEvent(new MailRequested(
                new MailMessage.EmailChangeNotice(to, locale(), firstName, newEmail)));
    }

    private static String locale() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getHeader(HttpHeaders.ACCEPT_LANGUAGE) != null) {
            return attributes.getRequest().getLocale().toLanguageTag();
        }
        return null;
    }
}
