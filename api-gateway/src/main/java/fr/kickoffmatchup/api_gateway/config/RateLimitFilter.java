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
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "kickoffmatchup:ratelimit:";

    /** Un * remplace un segment du chemin (bff ou mobile, verify ou resend…). */
    private static final List<PathPattern> LIMITED_PATHS = Stream.of(
            "/*/auth/login",
            "/*/auth/mfa/*",
            "/*/auth/register",
            "/*/auth/forgot-password",
            "/*/auth/resend-verification",
            "/*/auth/confirm-email-change",
            "/*/auth/me/email",
            "/*/auth/me/password-link",
            "/mobile/auth/social/*",
            "/mobile/auth/refresh")
            .map(PathPatternParser.defaultInstance::parse)
            .toList();

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
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        PathContainer path = PathContainer.parsePath(path(request));
        return LIMITED_PATHS.stream().noneMatch(pattern -> pattern.matches(path));
    }

    /**
     * Chemin décodé et en minuscules : /auth/social/Google ou un caractère encodé (%6c) ne doivent
     * ni échapper à la limite ni obtenir leur propre compteur.
     */
    private static String path(HttpServletRequest request) {
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        return path.toLowerCase(Locale.ROOT);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = KEY_PREFIX + request.getRemoteAddr() + ":" + path(request);
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
