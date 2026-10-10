package fr.kickoffmatchup.auth_server.repository;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.model.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
