package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.model.RefreshToken;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.RefreshTokenRepository;
import fr.kickoffmatchup.auth_server.util.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.jwt.refresh-ttl-days:30}")
    private long ttlDays;

    /** Fenêtre pendant laquelle un jeton déjà roté est toléré */
    @Value("${app.jwt.refresh-reuse-leeway-seconds:10}")
    private long leewaySeconds;

    public record Rotation(String refreshToken, User user) {}

    @Transactional
    public String create(User user) {
        return createInFamily(user, UUID.randomUUID().toString());
    }


    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String raw) {
        RefreshToken token = repository.findByTokenHash(Hashing.sha256Hex(raw))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN"));
        Instant now = Instant.now();

        if (token.getExpiresAt().isBefore(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
        }

        if (token.getRevokedAt() != null) {
            boolean concurrentUse = token.isRotated() && token.getRevokedAt().plusSeconds(leewaySeconds).isAfter(now);
            if (!concurrentUse) {
                repository.revokeFamily(token.getFamilyId(), now);
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
            }
        } else {
            repository.markRotated(token.getId(), now);
        }
        return new Rotation(createInFamily(token.getUser(), token.getFamilyId()), token.getUser());
    }

    @Transactional
    public void revokeFamilyOf(String raw) {
        repository.findByTokenHash(Hashing.sha256Hex(raw))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), Instant.now()));
    }

    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllForUser(user, Instant.now());
    }

    private String createInFamily(User user, String familyId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(RefreshToken.builder()
                .tokenHash(Hashing.sha256Hex(raw))
                .user(user)
                .familyId(familyId)
                .expiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS))
                .build());
        return raw;
    }
}
