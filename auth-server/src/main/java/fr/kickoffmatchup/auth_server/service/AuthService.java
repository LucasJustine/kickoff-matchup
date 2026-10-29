package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.dto.AuthDtos.RegisterRequest;
import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration VERIFY_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final VerificationTokenService tokens;
    private final EmailService emails;
    private final RefreshTokenService refreshTokens;

    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalize(request.email());
        String hash = encoder.encode(request.password());
        String firstName = request.firstName().trim();
        String lastName = request.lastName().trim();

        Optional<User> existing = users.findByEmail(email);
        User user;
        if (existing.isEmpty()) {
            user = users.save(User.builder()
                    .email(email).password(hash).firstName(firstName).lastName(lastName).build());
        } else if (!existing.get().isEmailVerified()) {
            user = existing.get();
            user.setPassword(hash);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            users.save(user);
        } else {
            emails.sendAccountAlreadyExists(existing.get().getEmail(), existing.get().getFirstName());
            return;
        }
        emails.sendVerification(user.getEmail(), user.getFirstName(),
                tokens.create(user, TokenType.EMAIL_VERIFICATION, VERIFY_TTL));
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = tokens.consume(token, TokenType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        users.save(user);
    }

    @Transactional
    public void resendVerification(String rawEmail) {
        users.findByEmail(normalize(rawEmail))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(u -> emails.sendVerification(u.getEmail(), u.getFirstName(),
                        tokens.create(u, TokenType.EMAIL_VERIFICATION, VERIFY_TTL)));
    }

    /** Vérifie email + mot de passe */
    @Transactional(readOnly = true)
    public User authenticate(String rawEmail, String password) {
        Optional<User> found = users.findByEmail(normalize(rawEmail));

        String hash = found.map(User::getPassword).filter(Objects::nonNull).orElse(dummyHash);
        boolean passwordMatches = encoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        User user = found.get();
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }
        return user;
    }

    @Transactional
    public void forgotPassword(String rawEmail) {
        users.findByEmail(normalize(rawEmail))
                .ifPresent(u -> emails.sendPasswordReset(u.getEmail(), u.getFirstName(),
                        tokens.create(u, TokenType.PASSWORD_RESET, RESET_TTL)));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = tokens.consume(token, TokenType.PASSWORD_RESET);
        user.setPassword(encoder.encode(newPassword));
        user.setEmailVerified(true); // il a prouvé qu'il contrôle la boîte mail
        users.save(user);
        refreshTokens.revokeAll(user); // toutes les connexions existantes (web et mobile) sont coupées
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
