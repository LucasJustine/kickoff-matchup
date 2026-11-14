package fr.kickoffmatchup.api_gateway.config;

import fr.kickoffmatchup.api_gateway.config.TokenBucketLimiter.Limit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Seau par utilisateur connecté, en plus des seaux par IP et par compte de {@link RateLimitFilter}.
 * Placé après la validation du JWT dans les chaînes de sécurité : l'identifiant vient d'un token vérifié,
 * un client ne peut donc pas en changer pour obtenir un seau neuf.
 */
public class UserRateLimitFilter extends OncePerRequestFilter {

    private final TokenBucketLimiter limiter;

    public UserRateLimitFilter(TokenBucketLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken user) {
            String path = TokenBucketLimiter.path(request);
            Limit limit = limiter.limitFor(path);
            if (limit != null && !limiter.allow(response, "user", user.getToken().getSubject(), path, limit)) {
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
