package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.LoginMethod;
import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.VerificationToken;
import fr.kickoffmatchup.auth_server.repository.UserIdentityRepository;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import fr.kickoffmatchup.auth_server.util.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Duration EMAIL_CHANGE_TTL = Duration.ofHours(24);
    private static final Duration DELETION_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final UserIdentityRepository identities;
    private final PasswordVerifier passwordVerifier;
    private final VerificationTokenService tokens;
    private final EmailService emails;
    private final RefreshTokenService refreshTokens;

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
    }

    @Transactional
    public User updateProfile(User user, String firstName, String lastName) {
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        return users.save(user);
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
        if (!enabled && user.isMfaEnabled() && hasPassword) {
            passwordVerifier.requireMatch(user, currentPassword);
        }
        user.setMfaEnabled(enabled);
        return users.save(user);
    }

    @Transactional
    public void requestEmailChange(User user, String rawNewEmail, String currentPassword) {
        if (user.getPassword() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PASSWORD_REQUIRED_FOR_EMAIL_CHANGE");
        }
        passwordVerifier.requireMatch(user, currentPassword);
        String newEmail = EmailAddress.normalize(rawNewEmail);
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
        passwordVerifier.requireMatch(user, currentPassword);
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
}
