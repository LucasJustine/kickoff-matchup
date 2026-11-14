package fr.kickoffmatchup.api_gateway.config;

import fr.kickoffmatchup.api_gateway.config.TokenBucketLimiter.Limit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Limite le débit des POST par IP, avant toute authentification. Le seau par utilisateur connecté
 * est appliqué plus loin par {@link UserRateLimitFilter}.
 * Premier filtre de l'application : une requête refusée ne touche ni la session ni Spring Security.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucketLimiter limiter;

    public RateLimitFilter(TokenBucketLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = TokenBucketLimiter.path(request);
        Limit limit = limiter.limitFor(path);
        if (limit != null && !limiter.allow(response, "ip", request.getRemoteAddr(), path, limit)) {
            return;
        }
        chain.doFilter(request, response);
    }
}
