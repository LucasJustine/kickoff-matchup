package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import fr.kickoffmatchup.auth_server.util.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final PasswordVerifier passwordVerifier;
    private final VerificationTokenService tokens;
    private final EmailService emails;
    private final RefreshTokenService refreshTokens;

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_NOT_SET");
        }
        passwordVerifier.requireMatch(user, currentPassword);
        user.setPassword(encoder.encode(newPassword));
        users.save(user);
        refreshTokens.revokeAll(user);
    }

    @Transactional
    public void requestPasswordLink(User user) {
        if (user.getPassword() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "PASSWORD_ALREADY_SET");
        }
        emails.sendSetPassword(user.getEmail(), user.getFirstName(), tokens.create(user, TokenType.PASSWORD_RESET, RESET_TTL));
    }

    @Transactional
    public void forgotPassword(String rawEmail) {
        users.findByEmail(EmailAddress.normalize(rawEmail))
                .ifPresent(u -> emails.sendPasswordReset(u.getEmail(), u.getFirstName(),
                        tokens.create(u, TokenType.PASSWORD_RESET, RESET_TTL)));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = tokens.consume(token, TokenType.PASSWORD_RESET);
        user.setPassword(encoder.encode(newPassword));
        user.setEmailVerified(true);
        users.save(user);
        refreshTokens.revokeAll(user);
    }
}
