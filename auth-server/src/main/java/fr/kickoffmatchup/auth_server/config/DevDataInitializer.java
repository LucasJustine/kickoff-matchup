package fr.kickoffmatchup.auth_server.config;

import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Compte de développement, créé uniquement avec le profil dev et seulement s'il n'existe pas.
 * Le mot de passe vient de DEV_ADMIN_PASSWORD : sans cette variable, aucun compte n'est créé.
 */
@Configuration
@Profile("dev")
@Slf4j
public class DevDataInitializer {

    private static final String EMAIL = "admin@kickoffmatchup.fr";

    @Bean
    CommandLineRunner devAdmin(UserRepository users, PasswordEncoder encoder,
                               @Value("${DEV_ADMIN_PASSWORD:}") String password) {
        return args -> {
            if (password.isBlank()) {
                log.info("DEV_ADMIN_PASSWORD absent : compte de développement non créé");
                return;
            }
            if (users.findByEmail(EMAIL).isPresent()) {
                return;
            }
            users.save(User.builder()
                    .email(EMAIL)
                    .firstName("Admin")
                    .lastName("KickoffMatchup")
                    .emailVerified(true)
                    .mfaEnabled(true)
                    .password(encoder.encode(password))
                    .build());
            log.info("Compte de développement créé : {}", EMAIL);
        };
    }
}
