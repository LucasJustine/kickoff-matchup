package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.dto.AuthDtos.RegisterRequest;
import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.VerificationToken;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import fr.kickoffmatchup.auth_server.util.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Duration VERIFY_TTL = Duration.ofHours(24);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final VerificationTokenService tokens;
    private final EmailService emails;

    @Transactional
    public void register(RegisterRequest request) {
        String email = EmailAddress.normalize(request.email());
        String hash = encoder.encode(request.password());
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();

        Optional<User> existing = users.findByEmail(email);
        User user;
        if (existing.isEmpty()) {
            user = users.save(User.builder()
                    .email(email).firstName(firstName).lastName(lastName).build());
        } else if (!existing.get().isEmailVerified()) {
            user = existing.get();
        } else {
            emails.sendAccountAlreadyExists(existing.get().getEmail(), existing.get().getFirstName());
            return;
        }
        emails.sendVerification(user.getEmail(), user.getFirstName(),
                tokens.create(user, TokenType.EMAIL_VERIFICATION, VERIFY_TTL, hash));
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verification = tokens.consumeToken(token, TokenType.EMAIL_VERIFICATION);
        User user = verification.getUser();
        if (verification.getPendingPasswordHash() != null) {
            user.setPassword(verification.getPendingPasswordHash());
        }
        user.setEmailVerified(true);
        users.save(user);
    }

    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmail(EmailAddress.normalize(rawEmail))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(u -> {
                    String pending = tokens.latestPendingPasswordHash(u, TokenType.EMAIL_VERIFICATION);
                    emails.sendVerification(u.getEmail(), u.getFirstName(),
                            tokens.create(u, TokenType.EMAIL_VERIFICATION, VERIFY_TTL, pending));
                });
    }
}
