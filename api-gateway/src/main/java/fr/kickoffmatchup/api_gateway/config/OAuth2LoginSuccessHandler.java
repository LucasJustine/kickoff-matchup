package fr.kickoffmatchup.api_gateway.config;

import fr.kickoffmatchup.api_gateway.security.SecurityConstants;
import fr.kickoffmatchup.api_gateway.utils.Session;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

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
            Authentication authentication) throws IOException, ServletException {

        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            String provider = oauthToken.getAuthorizedClientRegistrationId();
            OAuth2User oauth2User = oauthToken.getPrincipal();

            String idToken = null;
            if (oauth2User instanceof OidcUser oidcUser) {
                idToken = oidcUser.getIdToken().getTokenValue();
            }

            AuthResultDto authResult = authServerClient.post()
                    .uri("/auth/social-login")
                    .body(Map.of(
                            "provider", provider,
                            "idToken", idToken != null ? idToken : "",
                            "email", oauth2User.getAttribute("email") != null ? oauth2User.getAttribute("email") : ""))
                    .retrieve()
                    .body(AuthResultDto.class);

            if (authResult != null && authResult.tokens() != null) {
                Session.storeTokensInSession(request, authResult.tokens().accessToken(),
                        authResult.tokens().refreshToken());
            }
        }

        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/auth/callback-success");
    }

    private record TokenPairDto(String accessToken, String refreshToken) {
    }

    private record AuthResultDto(Object user, TokenPairDto tokens) {
    }
}