package fr.kickoffmatchup.api_gateway.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Locale;

@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.auth-server-url}") String authServerUrl,
            @Value("${app.auth-server-allow-http:false}") boolean allowHttp,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {
        if (!allowHttp && !authServerUrl.toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw new IllegalStateException(
                    "AUTH_SERVER_URL doit être en https (ou définir AUTH_SERVER_ALLOW_HTTP=true sur un réseau privé)");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(authServerUrl + "/.well-known/jwks.json")
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                JwtValidators.createDefaultWithIssuer(issuer),
                jwt -> jwt.getAudience() != null && jwt.getAudience().contains(audience)
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "audience", null))));
        return decoder;
    }
}
