package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.dto.AuthDtos.RegisterRequest;
import fr.kickoffmatchup.auth_server.enums.LoginMethod;
import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.VerificationToken;
import fr.kickoffmatchup.auth_server.repository.UserIdentityRepository;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final Duration EMAIL_CHANGE_TTL = Duration.ofHours(24);
    private static final Duration DELETION_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final VerificationTokenService tokens;
    private final EmailService emails;
    private final RefreshTokenService refreshTokens;
    private final UserIdentityRepository identities;

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
        users.findByEmail(normalize(rawEmail))
                .filter(u -> !u.isEmailVerified())
                .ifPresent(u -> {
                    String pending = tokens.latestPendingPasswordHash(u, TokenType.EMAIL_VERIFICATION);
                    emails.sendVerification(u.getEmail(), u.getFirstName(),
                            tokens.create(u, TokenType.EMAIL_VERIFICATION, VERIFY_TTL, pending));
                });
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
    public User setMfa(User user, boolean enabled, String currentPassword, LoginMethod loginMethod) {
        if (loginMethod != LoginMethod.PASSWORD) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MFA_UNAVAILABLE_WITH_PROVIDER");
        }
        boolean hasPassword = user.getPassword() != null;
        if (enabled && !hasPassword) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_REQUIRED_FOR_MFA");
        }
        if (!enabled && user.isMfaEnabled() && hasPassword
                && (currentPassword == null || !encoder.matches(currentPassword, user.getPassword()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CURRENT_PASSWORD");
        }
        user.setMfaEnabled(enabled);
        return users.save(user);
    }

    @Transactional
    public User updateProfile(User user, String firstName, String lastName) {
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        return users.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_NOT_SET");
        }
        requireCurrentPassword(user, currentPassword);
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
    public void requestEmailChange(User user, String rawNewEmail, String currentPassword) {
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_REQUIRED_FOR_EMAIL_CHANGE");
        }
        if (currentPassword == null || !encoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CURRENT_PASSWORD");
        }
        String newEmail = normalize(rawNewEmail);
        if (newEmail.equals(user.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_UNCHANGED");
        }
        Optional<User> owner = users.findByEmail(newEmail);
        if (owner.isPresent()) {
            emails.sendAccountAlreadyExists(owner.get().getEmail(), owner.get().getFirstName());
            return;
        }
        String token = tokens.create(user, TokenType.EMAIL_CHANGE, EMAIL_CHANGE_TTL, null, newEmail);
        emails.sendEmailChange(newEmail, user.getFirstName(), token);
        emails.sendEmailChangeNotice(user.getEmail(), user.getFirstName(), newEmail);
    }

    @Transactional
    public void confirmEmailChange(String token) {
        VerificationToken verification = tokens.consumeToken(token, TokenType.EMAIL_CHANGE);
        String newEmail = verification.getPendingEmail();
        if (newEmail == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN");
        }
        User user = verification.getUser();
        boolean taken = users.findByEmail(newEmail).filter(other -> !other.getId().equals(user.getId())).isPresent();
        if (taken) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED");
        }
        user.setEmail(newEmail);
        user.setEmailVerified(true);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED");
        }
    }

    /** @return true si le compte est supprimé, false si un email de confirmation a été envoyé (compte sans mot de passe). */
    @Transactional
    public boolean deleteAccount(User user, String currentPassword) {
        if (user.getPassword() == null) {
            emails.sendAccountDeletion(user.getEmail(), user.getFirstName(),
                    tokens.create(user, TokenType.ACCOUNT_DELETION, DELETION_TTL));
            return false;
        }
        requireCurrentPassword(user, currentPassword);
        erase(user);
        return true;
    }

    @Transactional
    public void confirmAccountDeletion(String token) {
        erase(tokens.consume(token, TokenType.ACCOUNT_DELETION));
    }

    private void erase(User user) {
        refreshTokens.deleteAll(user);
        tokens.deleteAll(user);
        identities.deleteAllForUser(user);
        users.delete(user);
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (user.getPassword() != null
                && (currentPassword == null || !encoder.matches(currentPassword, user.getPassword()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CURRENT_PASSWORD");
        }
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
