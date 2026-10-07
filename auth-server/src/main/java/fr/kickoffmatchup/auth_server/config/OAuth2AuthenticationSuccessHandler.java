package fr.kickoffmatchup.auth_server.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import fr.kickoffmatchup.auth_server.service.AuthTokenService;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthTokenService authTokenService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public OAuth2AuthenticationSuccessHandler(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Value("${app.frontendUrl:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String email = authentication.getPrincipal() instanceof OidcUser oidcUser
                ? oidcUser.getEmail()
                : authentication.getName();

        Authentication applicationAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        User.withUsername(email)
                                .password("")
                                .authorities(authentication.getAuthorities())
                                .build(),
                        null,
                        authentication.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(applicationAuthentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        authTokenService.issue(applicationAuthentication, request);
        String targetUrl = frontendUrl;

        if (request.getCookies() != null) {
            for (Cookie requestCookie : request.getCookies()) {
                if ("redirect_uri".equals(requestCookie.getName())) {
                    String requestedPath = requestCookie.getValue();
                    if (requestedPath.startsWith("/")) {
                        targetUrl = frontendUrl + requestedPath;
                    }

                    Cookie clearCookie = new Cookie("redirect_uri", "");
                    clearCookie.setPath("/");
                    clearCookie.setMaxAge(0);
                    response.addCookie(clearCookie);
                    break;
                }
            }
        }

        response.sendRedirect(targetUrl);
    }
}
