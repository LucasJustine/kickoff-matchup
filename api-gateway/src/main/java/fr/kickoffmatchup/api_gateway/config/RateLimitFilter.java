package fr.kickoffmatchup.api_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "kickoffmatchup:ratelimit:";

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/bff/auth/login",
            "/bff/auth/mfa/verify",
            "/bff/auth/mfa/resend",
            "/bff/auth/register",
            "/bff/auth/forgot-password",
            "/bff/auth/resend-verification",
            "/bff/auth/me/email",
            "/bff/auth/me/password-link",
            "/bff/auth/confirm-email-change");

    private final StringRedisTemplate redis;
    private final int maxRequests;
    private final Duration window;

    public RateLimitFilter(StringRedisTemplate redis,
            @Value("${app.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redis = redis;
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = KEY_PREFIX + request.getRemoteAddr() + ":" + request.getRequestURI();
        try {
            Long count = redis.opsForValue().increment(key);
            Long ttl = redis.getExpire(key);
            if (ttl == null || ttl < 0) {
                redis.expire(key, window);
                ttl = window.toSeconds();
            }
            if (count != null && count > maxRequests) {
                reject(response, ttl);
                return;
            }
        } catch (DataAccessException e) {
            log.warn("Rate limiting indisponible (Redis) : {}", e.getMessage());
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(Math.max(retryAfterSeconds, 1)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Trop de tentatives, réessayez plus tard.\"}");
    }
}
