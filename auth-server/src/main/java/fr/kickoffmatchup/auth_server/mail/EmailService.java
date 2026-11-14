package fr.kickoffmatchup.auth_server.mail;

import fr.kickoffmatchup.mail_contract.MailMessage;
import fr.kickoffmatchup.auth_server.util.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int MAX_PER_HOUR = 10;
    private static final Duration WINDOW = Duration.ofHours(1);
    private static final String RATE_PREFIX = "mail:rate:";

    public record MailRequested(MailMessage message) {}

    private final ApplicationEventPublisher events;
    private final StringRedisTemplate redis;

    public void sendVerification(String to, String firstName, String token) {
        publish(new MailMessage.VerifyEmail(to, locale(), firstName, token));
    }

    public void sendPasswordReset(String to, String firstName, String token) {
        publish(new MailMessage.PasswordReset(to, locale(), firstName, token));
    }

    public void sendMfaCode(String to, String code) {
        publish(new MailMessage.MfaCode(to, locale(), code));
    }

    public void sendAccountAlreadyExists(String to, String firstName) {
        publish(new MailMessage.AccountAlreadyExists(to, locale(), firstName));
    }

    public void sendEmailChange(String to, String firstName, String token) {
        publish(new MailMessage.EmailChange(to, locale(), firstName, token));
    }

    public void sendEmailChangeNotice(String to, String firstName, String newEmail) {
        publish(new MailMessage.EmailChangeNotice(to, locale(), firstName, newEmail));
    }

    public void sendSetPassword(String to, String firstName, String token) {
        publish(new MailMessage.SetPassword(to, locale(), firstName, token));
    }

    public void sendAccountDeletion(String to, String firstName, String token) {
        publish(new MailMessage.AccountDeletion(to, locale(), firstName, token));
    }

    private void publish(MailMessage message) {
        if (!withinLimit(message.to())) {
            log.warn("Plafond d'emails atteint pour une adresse : {} non envoyé",
                    message.getClass().getSimpleName());
            return;
        }
        events.publishEvent(new MailRequested(message));
    }

    private boolean withinLimit(String to) {
        String key = RATE_PREFIX + Hashing.sha256Hex(to);
        try {
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, WINDOW);
            }
            return count == null || count <= MAX_PER_HOUR;
        } catch (DataAccessException e) {
            log.warn("Limite d'emails indisponible (Redis) : {}", e.getMessage());
            return true;
        }
    }

    private static String locale() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && attributes.getRequest().getHeader(HttpHeaders.ACCEPT_LANGUAGE) != null) {
            return attributes.getRequest().getLocale().toLanguageTag();
        }
        return null;
    }
}
