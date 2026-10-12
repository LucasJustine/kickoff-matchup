package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.util.Hashing;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MfaService {

    public static final Duration TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;
    private static final String PREFIX = "mfa:challenge:";

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public record Challenge(String id, String code) {}

    public Challenge start(Long userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String code = String.format("%06d", random.nextInt(1_000_000));

        String key = PREFIX + id;
        redis.opsForHash().putAll(key, Map.of(
                "userId", userId.toString(),
                "codeHash", Hashing.sha256Hex(code),
                "attempts", "0"));
        redis.expire(key, TTL);
        return new Challenge(id, code);
    }

    public Long verify(String challengeId, String code) {
        String key = PREFIX + challengeId;

        Object storedHash = redis.opsForHash().get(key, "codeHash");
        if (storedHash == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        }

        Long attempts = redis.opsForHash().increment(key, "attempts", 1);
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            redis.delete(key);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_TOO_MANY_ATTEMPTS");
        }

        boolean valid = MessageDigest.isEqual(
                Hashing.sha256Hex(code).getBytes(StandardCharsets.UTF_8),
                storedHash.toString().getBytes(StandardCharsets.UTF_8));
        if (!valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_INVALID_CODE");
        }

        Object userId = redis.opsForHash().get(key, "userId");
        redis.delete(key);
        return Long.valueOf(userId.toString());
    }
}
