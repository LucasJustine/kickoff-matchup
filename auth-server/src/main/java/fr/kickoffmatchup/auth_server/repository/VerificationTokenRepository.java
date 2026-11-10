package fr.kickoffmatchup.auth_server.repository;

import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);

    Optional<VerificationToken> findFirstByUserAndTypeOrderByIdDesc(User user, TokenType type);

    void deleteByUserAndType(User user, TokenType type);

    @Modifying
    @Query("delete from VerificationToken t where t.user = :user")
    int deleteAllForUser(@Param("user") User user);

    @Modifying
    @Query("update VerificationToken t set t.usedAt = :now where t.id = :id and t.usedAt is null")
    int markUsed(@Param("id") Long id, @Param("now") Instant now);
}
