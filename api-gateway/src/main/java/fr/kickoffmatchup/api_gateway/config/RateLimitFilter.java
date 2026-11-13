package fr.kickoffmatchup.api_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String KEY_PREFIX = "kickoffmatchup:ratelimit:bucket:";

    /**
     * https://redis.io/docs/latest/develop/use-cases/rate-limiter/java-jedis/
     * Seau à jetons, exécuté de façon atomique par Redis. Le seau se remplit en continu
     * (capacity jetons par période) et chaque requête consomme un jeton.
     * KEYS[1] = seau ; ARGV[1] = capacité ; ARGV[2] = période en ms.
     * Renvoie { autorisé (0/1), jetons restants, attente en ms avant le prochain jeton }.
     */
    private static final String TOKEN_BUCKET_LUA = """
            local capacity = tonumber(ARGV[1])
            local period = tonumber(ARGV[2])
            local time = redis.call('TIME')
            local now = time[1] * 1000 + math.floor(time[2] / 1000)

            local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'refreshed')
            local tokens = tonumber(bucket[1]) or capacity
            local refreshed = tonumber(bucket[2]) or now

            tokens = math.min(capacity, tokens + math.max(0, now - refreshed) * capacity / period)

            local allowed = 0
            local retry = 0
            if tokens >= 1 then
                allowed = 1
                tokens = tokens - 1
            else
                retry = math.ceil((1 - tokens) * period / capacity)
            end

            redis.call('HSET', KEYS[1], 'tokens', tostring(tokens), 'refreshed', tostring(now))
            redis.call('PEXPIRE', KEYS[1], period)
            return { allowed, math.floor(tokens), retry }
            """;

    @SuppressWarnings("rawtypes")
    private static final RedisScript<List> TOKEN_BUCKET = RedisScript.of(TOKEN_BUCKET_LUA, List.class);

    private record Limit(PathPattern pattern, int capacity, long periodMillis) {
    }

    private final StringRedisTemplate redis;
    private final List<Limit> limits;

    public RateLimitFilter(StringRedisTemplate redis, RateLimitProperties properties) {
        this.redis = redis;
        this.limits = properties.rules().stream()
                .map(rule -> new Limit(
                        PathPatternParser.defaultInstance.parse(rule.path()),
                        rule.capacity(),
                        rule.period().toMillis()))
                .toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = path(request);
        Limit limit = limitFor(path);
        
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = KEY_PREFIX + request.getRemoteAddr() + ":" + path;
        try {
            List<?> result = redis.execute(TOKEN_BUCKET, List.of(key),
                    String.valueOf(limit.capacity()), String.valueOf(limit.periodMillis()));
            if (result != null && result.size() == 3) {
                response.setHeader(REMAINING_HEADER, String.valueOf(number(result.get(1))));
                if (number(result.get(0)) != 1) {
                    reject(response, number(result.get(2)));
                    return;
                }
            }
        } catch (DataAccessException e) {
            log.warn("Rate limiting indisponible (Redis) : {}", e.getMessage());
        }
        chain.doFilter(request, response);
    }

    private Limit limitFor(String path) {
        PathContainer container;
        try {
            container = PathContainer.parsePath(path);
        } catch (IllegalArgumentException e) {
            return null;
        }
        for (Limit limit : limits) {
            if (limit.pattern().matches(container)) {
                return limit;
            }
        }
        return null;
    }

    /**
     * Chemin décodé et en minuscules : /auth/social/Google ou un caractère encodé (%6c) ne doivent
     * ni échapper à la limite ni obtenir leur propre seau.
     */
    private static String path(HttpServletRequest request) {
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        return path.toLowerCase(Locale.ROOT);
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
    }

    private void reject(HttpServletResponse response, long retryAfterMillis) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(Math.max((retryAfterMillis + 999) / 1000, 1)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Trop de tentatives, réessayez plus tard.\"}");
    }
}
