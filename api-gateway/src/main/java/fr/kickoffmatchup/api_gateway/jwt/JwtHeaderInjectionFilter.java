package fr.kickoffmatchup.api_gateway.jwt;

import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class JwtHeaderInjectionFilter extends OncePerRequestFilter {

    public static final String ACCESS_TOKEN_ATTRIBUTE =
            JwtHeaderInjectionFilter.class.getName() + ".ACCESS_TOKEN";

    private static final Logger log = LoggerFactory.getLogger(JwtHeaderInjectionFilter.class);
    private static final String SESSION_ACCESS_TOKEN = "ACCESS_TOKEN";

    /** On renouvelle un peu avant l'expiration pour ne pas expirer entre la gateway et le service aval. */
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);

    private static final int LOCK_STRIPES = 64;

    private final RedisSessionRepository sessionRepository;
    private final RestClient authServerClient;

 
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public JwtHeaderInjectionFilter(
            RedisSessionRepository sessionRepository,
            RestClient.Builder restClientBuilder,
            @Value("${auth-server.url:http://localhost:9000}") String authServerUrl) {
        this.sessionRepository = sessionRepository;
        this.authServerClient = restClientBuilder.baseUrl(authServerUrl).build();
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new ReentrantLock();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromSession(request);

        if (token != null) {
            Instant expiresAt = expiresAt(token);
            Instant now = Instant.now();

            if (expiresAt != null && expiresAt.isBefore(now.plus(REFRESH_MARGIN))) {
                String renewed = renew(request, token);
                if (renewed != null) {
                    token = renewed;
                } else if (expiresAt.isBefore(now)) {
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session expiree");
                    return;
                }
            }
        }

        final String resolvedToken = token;

        if (request.getRequestURI().startsWith("/api/") && resolvedToken == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session non authentifiee");
            return;
        }

        if (resolvedToken != null) {
            request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, resolvedToken);

            request = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return "Bearer " + resolvedToken;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        return Collections.enumeration(
                                List.of("Bearer " + resolvedToken));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> headerNames = Collections.list(super.getHeaderNames());
                    if (headerNames.stream()
                            .noneMatch("Authorization"::equalsIgnoreCase)) {
                        headerNames.add("Authorization");
                    }
                    return Collections.enumeration(headerNames);
                }
            };
        }
        filterChain.doFilter(request, response);
    }


    private String renew(HttpServletRequest request, String currentToken) {
        var servletSession = request.getSession(false);
        if (servletSession == null) {
            return null;
        }
        String sessionId = servletSession.getId();

        ReentrantLock lock = locks[Math.floorMod(sessionId.hashCode(), LOCK_STRIPES)];
        lock.lock();
        try {
            String latest = readTokenFromRedis(sessionId);
            if (isRenewed(latest, currentToken)) {
                return latest;
            }

            callRefreshEndpoint(request);

            latest = readTokenFromRedis(sessionId);
            return isRenewed(latest, currentToken) ? latest : null;
        } finally {
            lock.unlock();
        }
    }

    private void callRefreshEndpoint(HttpServletRequest request) {
        String cookie = request.getHeader("Cookie");
        if (cookie == null) {
            return;
        }

        try {
            authServerClient.post()
                .uri("/auth/refresh")
                .header("Cookie", cookie)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            log.debug("Refresh du token impossible : {}", exception.getMessage());
        }
    }

    private String extractTokenFromSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null && session.getAttribute(SESSION_ACCESS_TOKEN) instanceof String token) {
            return token;
        }
        return null;
    }

    private String readTokenFromRedis(String sessionId) {
        Session session = sessionRepository.findById(sessionId);
        if (session == null) {
            return null;
        }
        return session.getAttribute(SESSION_ACCESS_TOKEN) instanceof String token ? token : null;
    }

    private boolean isRenewed(String candidate, String currentToken) {
        if (candidate == null || candidate.equals(currentToken)) {
            return false;
        }
        Instant expiresAt = expiresAt(candidate);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    /**
     * Date d'expiration lue dans le JWT, sans valider la signature (le token vient de notre
     * propre session ; la validation reste faite par le resource server). Null si illisible :
     * on ne tente alors pas de refresh et le resource server rejettera le token.
     */
    private Instant expiresAt(String token) {
        try {
            Date expiration = SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime();
            return expiration == null ? null : expiration.toInstant();
        } catch (ParseException exception) {
            return null;
        }
    }
}