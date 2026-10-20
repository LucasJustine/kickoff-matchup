package fr.kickoffmatchup.api_gateway.config;

import fr.kickoffmatchup.api_gateway.config.ReturnUrl;
import fr.kickoffmatchup.api_gateway.utils.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Fin du flux OAuth, en redirection pleine page. Le nom de l'enregistrement (google, apple…) sert de {provider}
 * côté auth service.
 *
 * - succès : tokens de l'auth service rangés en session, puis redirection vers frontend-url + returnUrl
 *            (le chemin demandé au départ, sinon /welcome) ;
 * - échec  : redirection vers /login?error=CODE en conservant returnUrl, pour que l'utilisateur puisse réessayer.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);
    private static final String FAILED = "SOCIAL_LOGIN_FAILED";
    private static final String DEFAULT_RETURN_URL = "/welcome";

    private final RestClient authServerClient;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            RestClient.Builder restClientBuilder,
            @Value("${auth-server.url:http://localhost:9000}") String authServerUrl,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.authServerClient = restClientBuilder.baseUrl(authServerUrl).build();
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        // Seuls les fournisseurs OIDC (qui fournissent un ID token) sont gérés.
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)
                || !(oauthToken.getPrincipal() instanceof OidcUser oidcUser)) {
            redirectToLogin(request, response, "UNSUPPORTED_PROVIDER");
            return;
        }
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        try {
            // Même contrat que POST /auth/social/{provider} { idToken } de l'auth service.
            AuthResultDto result = authServerClient.post()
                    .uri("/auth/social/{provider}", provider)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("idToken", oidcUser.getIdToken().getTokenValue()))
                    .retrieve()
                    .body(AuthResultDto.class);

            if (result == null || result.tokens() == null || result.tokens().accessToken() == null) {
                throw new RestClientException("Réponse de l'auth service sans tokens");
            }

            Session.storeTokensInSession(request, result.tokens().accessToken(), result.tokens().refreshToken());

            String returnUrl = takeReturnUrl(request);
            response.sendRedirect(frontendUrl + (returnUrl != null ? returnUrl : DEFAULT_RETURN_URL));

        } catch (RestClientResponseException e) {
            log.warn("Connexion sociale ({}) : l'auth service a répondu HTTP {} corps='{}'",
                    provider, e.getStatusCode().value(), e.getResponseBodyAsString());
            redirectToLogin(request, response, codeOf(e));
        } catch (RestClientException e) {
            log.warn("Connexion sociale ({}) impossible : {}", provider, e.getMessage());
            redirectToLogin(request, response, FAILED);
        }
    }

    /** Refus du fournisseur, state invalide… */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        redirectToLogin(request, response, FAILED);
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response, String code)
            throws IOException {
        String url = frontendUrl + "/login?error=" + encode(code);
        String returnUrl = takeReturnUrl(request);
        if (returnUrl != null) {
            url += "&returnUrl=" + encode(returnUrl);
        }
        response.sendRedirect(url);
    }

    private String takeReturnUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(ReturnUrl.SESSION_ATTRIBUTE);
        session.removeAttribute(ReturnUrl.SESSION_ATTRIBUTE);
        return value instanceof String raw ? ReturnUrl.sanitize(raw) : null;
    }

    /** Code d'erreur de l'auth service (OAUTH_EMAIL_NOT_VERIFIED, INVALID_SOCIAL_TOKEN…), sinon SOCIAL_LOGIN_FAILED. */
    private String codeOf(RestClientResponseException e) {
        try {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            return body != null && body.code() != null ? body.code() : FAILED;
        } catch (Exception ignored) {
            return FAILED;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record TokenPairDto(String accessToken, String refreshToken) {
    }

    private record AuthResultDto(Object user, TokenPairDto tokens) {
    }

    private record ErrorBody(String code) {
    }
}
