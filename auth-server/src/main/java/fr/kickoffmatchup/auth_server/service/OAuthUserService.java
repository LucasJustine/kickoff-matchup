package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.UserIdentity;
import fr.kickoffmatchup.auth_server.repository.UserIdentityRepository;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthUserService {

    private final UserRepository users;
    private final UserIdentityRepository identities;

    /**
     * 1) identité déjà liée (provider + sub)  -> on renvoie son utilisateur
     * 2) sinon, email vérifié par le provider -> on rattache à l'utilisateur existant ou on le crée
     */
    @Transactional
    public User findOrCreate(SocialProfile p) {
        Optional<UserIdentity> known = identities.findByProviderAndProviderUserId(p.provider(), p.subject());
        if (known.isPresent()) {
            return known.get().getUser();
        }

        if (p.email() == null || p.email().isBlank() || !p.emailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "OAUTH_EMAIL_NOT_VERIFIED");
        }
        String email = p.email().trim().toLowerCase(Locale.ROOT);

        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            user = users.save(User.builder()
                    .email(email)
                    .firstName(firstName(p, email))
                    .lastName(p.familyName() == null ? "" : p.familyName().trim())
                    .emailVerified(true)
                    .build());
        } else if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setPassword(null);
            users.save(user);
        }

        identities.save(UserIdentity.builder()
                .user(user)
                .provider(p.provider())
                .providerUserId(p.subject())
                .providerEmail(email)
                .build());
        return user;
    }

    private static String firstName(SocialProfile p, String email) {
        if (p.givenName() != null && !p.givenName().isBlank()) return p.givenName().trim();
        if (p.name() != null && !p.name().isBlank()) return p.name().trim();
        return email.substring(0, email.indexOf('@'));
    }
}
