package fr.kickoffmatchup.auth_server.repository;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, Long> {
    Optional<UserIdentity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    @Modifying
    @Query("delete from UserIdentity i where i.user = :user")
    int deleteAllForUser(@Param("user") User user);
}
