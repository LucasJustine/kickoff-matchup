package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.TokenType;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.model.VerificationToken;
import fr.kickoffmatchup.auth_server.repository.VerificationTokenRepository;
import fr.kickoffmatchup.auth_server.util.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository repository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public String create(User user, TokenType type, Duration ttl) {
        repository.deleteByUserAndType(user, type);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(VerificationToken.builder()
                .tokenHash(Hashing.sha256Hex(raw))
                .user(user)
                .type(type)
                .expiresAt(Instant.now().plus(ttl))
                .build());
        return raw;
    }

    @Transactional
    public User consume(String raw, TokenType type) {
        VerificationToken token = repository.findByTokenHashAndType(Hashing.sha256Hex(raw), type)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN"));

        Instant now = Instant.now();
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN");
        }
        if (repository.markUsed(token.getId(), now) == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN");
        }
        return token.getUser();
    }
}
