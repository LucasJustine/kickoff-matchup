package fr.kickoffmatchup.api_gateway.jwt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jwt.SignedJWT;

import fr.kickoffmatchup.api_gateway.security.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
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


    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayTokenPair(String accessToken, String refreshToken) {}

    private record RefreshRequest(String refreshToken) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GatewayAuthResult(GatewayTokenPair tokens) {}
    
    private static final Logger log = LoggerFactory.getLogger(JwtHeaderInjectionFilter.class);

    /** On renouvelle un peu avant l'expiration pour ne pas expirer entre la gateway et le service aval. */
    private static final Duration REFRESH_MARGIN = Duration.ofSeconds(30);

    private static final int LOCK_STRIPES = 64;

    private final RedisSessionRepository sessionRepository;
    private final RestClient authServerClient;

 
    private final ReentrantLock[] locks = new ReentrantLock[LOCK_STRIPES];

    public JwtHeaderInjectionFilter(
            RedisSessionRepository sessionRepository,
            RestClient.Builder restClientBuilder,
            @Value("${app.auth-server-url}") String authServerUrl) {
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
            System.out.println("Expires at: " + expiresAt);
            System.out.println("Now: " + now);
            if (expiresAt != null && expiresAt.isBefore(now.plus(REFRESH_MARGIN))) {
                String renewed = renew(request, token);
                if (renewed != null) {
                    token = renewed;
                } else if (expiresAt.isBefore(now)) {
                    // Expiré et non renouvelable : pas de Bearer. La chaîne de sécurité tranche (401 sur une route
                    // protégée) ; une route publique (login, providers…) reste utilisable pour se reconnecter.
                    token = null;
                }
            }
        }

        final String resolvedToken = token;

        if (request.getRequestURI().startsWith("/api/") && resolvedToken == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session non authentifiee");
            return;
        }

        if (resolvedToken != null) {
            request.setAttribute(SecurityConstants.REQUEST_ACCESS_TOKEN_ATTRIBUTE, resolvedToken);

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
            String latest = (String) servletSession.getAttribute(SecurityConstants.SESSION_ACCESS_TOKEN);
            if (isRenewed(latest, currentToken)) {
                return latest;
            }

            if (!(servletSession.getAttribute(SecurityConstants.SESSION_REFRESH_TOKEN) instanceof String refreshToken)) {
                return null;
            }

            try {
                GatewayTokenPair pair = callRefreshEndpoint(refreshToken);
                if (pair == null || pair.accessToken() == null) {
                    return null;
                }
                servletSession.setAttribute(SecurityConstants.SESSION_ACCESS_TOKEN, pair.accessToken());
                // Le refresh token tourne : on garde le nouveau, sinon le prochain refresh rejouerait l'ancien.
                if (pair.refreshToken() != null) {
                    servletSession.setAttribute(SecurityConstants.SESSION_REFRESH_TOKEN, pair.refreshToken());
                }
                return pair.accessToken();

            } catch (HttpClientErrorException exception) {
                log.warn("Refresh refusé par l'auth server : HTTP {} {}",
                        exception.getStatusCode().value(), exception.getResponseBodyAsString());
                servletSession.removeAttribute(SecurityConstants.SESSION_ACCESS_TOKEN);
                servletSession.removeAttribute(SecurityConstants.SESSION_REFRESH_TOKEN);
                return null;
            } catch (RestClientException exception) {
                log.warn("Refresh du token impossible : {}", exception.getMessage());
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    private GatewayTokenPair callRefreshEndpoint(String refreshToken) {
        GatewayAuthResult body = authServerClient.post()
                .uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new RefreshRequest(refreshToken))
                .retrieve()
                .body(GatewayAuthResult.class);
        return body == null ? null : body.tokens();
    }


    private String extractTokenFromSession(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null && session.getAttribute(SecurityConstants.SESSION_ACCESS_TOKEN) instanceof String token) {
            return token;
        }
        return null;
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