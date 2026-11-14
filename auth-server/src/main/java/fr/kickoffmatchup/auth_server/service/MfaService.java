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
import java.time.Instant;
import java.util.Base64;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class MfaService {

    public static final Duration CODE_TTL = Duration.ofMinutes(5);

    public static final Duration CHALLENGE_TTL = Duration.ofMinutes(15);

    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(30);

    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_RESENDS = 3;
    private static final String PREFIX = "mfa:challenge:";
    private static final String COOLDOWN_SUFFIX = ":cooldown";

    private static final String FAILURES_PREFIX = "mfa:failures:";
    private static final int MAX_USER_FAILURES = 10;
    private static final Duration FAILURES_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public record Challenge(String id, String code) {}

    public record Resend(Long userId, String code) {}

    public Challenge start(Long userId) {
        String failures = redis.opsForValue().get(FAILURES_PREFIX + userId);
        if (failures != null && Long.parseLong(failures) >= MAX_USER_FAILURES) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_TOO_MANY_ATTEMPTS");
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String code = newCode();

        String key = PREFIX + id;
        redis.opsForHash().putAll(key, Map.of(
                "userId", userId.toString(),
                "codeHash", Hashing.sha256Hex(code),
                "codeExpiresAt", expiresAt(),
                "attempts", "0",
                "resends", "0"));
        redis.expire(key, CHALLENGE_TTL);
        startCooldown(id);
        return new Challenge(id, code);
    }

    public Long verify(String challengeId, String code) {
        String key = PREFIX + challengeId;

        Object storedHash = redis.opsForHash().get(key, "codeHash");
        if (storedHash == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        }

        Object codeExpiresAt = redis.opsForHash().get(key, "codeExpiresAt");
        if (codeExpiresAt == null || Instant.now().getEpochSecond() > Long.parseLong(codeExpiresAt.toString())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CODE_EXPIRED");
        }

        Long attempts = redis.opsForHash().increment(key, "attempts", 1);
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_TOO_MANY_ATTEMPTS");
        }

        boolean valid = MessageDigest.isEqual(
                Hashing.sha256Hex(code).getBytes(StandardCharsets.UTF_8),
                storedHash.toString().getBytes(StandardCharsets.UTF_8));
        Object userId = redis.opsForHash().get(key, "userId");
        if (!valid) {
            if (userId != null) {
                recordFailure(userId.toString());
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_INVALID_CODE");
        }

        if (userId == null || !Boolean.TRUE.equals(redis.delete(key))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        }
        redis.delete(key + COOLDOWN_SUFFIX);
        return Long.valueOf(userId.toString());
    }

    private void recordFailure(String userId) {
        String failuresKey = FAILURES_PREFIX + userId;
        Long failures = redis.opsForValue().increment(failuresKey);
        if (failures != null && failures == 1) {
            redis.expire(failuresKey, FAILURES_WINDOW);
        }
    }


    public Resend resend(String challengeId) {
        String key = PREFIX + challengeId;

        Object userId = redis.opsForHash().get(key, "userId");
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED");
        }

        Long resends = redis.opsForHash().increment(key, "resends", 1);
        if (resends != null && resends > MAX_RESENDS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_TOO_MANY_RESENDS");
        }

        if (!startCooldown(challengeId)) {
            redis.opsForHash().increment(key, "resends", -1);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MFA_RESEND_TOO_SOON");
        }

        String code = newCode();
        redis.opsForHash().putAll(key, Map.of(
                "codeHash", Hashing.sha256Hex(code),
                "codeExpiresAt", expiresAt()));
        redis.expire(key, CHALLENGE_TTL);
        return new Resend(Long.valueOf(userId.toString()), code);
    }

    private boolean startCooldown(String challengeId) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(PREFIX + challengeId + COOLDOWN_SUFFIX, "1", RESEND_COOLDOWN));
    }

    private String newCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String expiresAt() {
        return Long.toString(Instant.now().plus(CODE_TTL).getEpochSecond());
    }
}
