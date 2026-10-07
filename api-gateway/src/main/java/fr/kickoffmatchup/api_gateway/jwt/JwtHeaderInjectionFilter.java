package fr.kickoffmatchup.api_gateway.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtHeaderInjectionFilter extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final RedisSessionRepository sessionRepository;
    private final RestClient authServerClient;

    public JwtHeaderInjectionFilter(
            JwtDecoder jwtDecoder,
            RedisSessionRepository sessionRepository,
            RestClient.Builder restClientBuilder,
            @Value("${auth-server.url:http://localhost:9000}") String authServerUrl) {
        this.jwtDecoder = jwtDecoder;
        this.sessionRepository = sessionRepository;
        this.authServerClient = restClientBuilder.baseUrl(authServerUrl).build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractTokenFromSession(request);
        if (token != null && isExpired(token)) {
            if (!refreshToken(request)) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session expiree");
                return;
            }
            token = extractTokenFromRedis(request);
        }

        final String resolvedToken = token;

        if (request.getRequestURI().startsWith("/api/")
                && resolvedToken == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Session non authentifiee");
            return;
        }

        if (resolvedToken != null) {
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

    private String extractTokenFromSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            Object accessToken = request.getSession(false).getAttribute("ACCESS_TOKEN");
            if (accessToken instanceof String token) {
                return token;
            }
        }
        return null;
    }

    private String extractTokenFromRedis(HttpServletRequest request) {
        var servletSession = request.getSession(false);
        if (servletSession == null) {
            return null;
        }

        Session session = sessionRepository.findById(servletSession.getId());
        if (session == null) {
            return null;
        }

        Object accessToken = session.getAttribute("ACCESS_TOKEN");
        return accessToken instanceof String token ? token : null;
    }

    private boolean isExpired(String token) {
        try {
            jwtDecoder.decode(token);
            return false;
        } catch (JwtValidationException exception) {
            return exception.getErrors().stream()
                    .anyMatch(error -> "invalid_token".equals(error.getErrorCode())
                            && error.getDescription() != null
                            && error.getDescription().toLowerCase().contains("expired"));
        }
    }

    private boolean refreshToken(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null || request.getHeader("Cookie") == null) {
            return false;
        }

        try {
            authServerClient.post()
                    .uri("/auth/refresh")
                    .header("Cookie", request.getHeader("Cookie"))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientResponseException exception) {
            return false;
        }
    }
}