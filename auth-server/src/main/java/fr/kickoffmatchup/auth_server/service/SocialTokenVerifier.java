package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;


@Service
public class SocialTokenVerifier {

    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final JwtDecoder google;

    public SocialTokenVerifier(@Value("${app.social.google.client-ids}") List<String> googleClientIds) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                new JwtTimestampValidator(),
                jwt -> GOOGLE_ISSUERS.contains(jwt.getClaimAsString("iss"))
                        ? OAuth2TokenValidatorResult.success() : failure("issuer"),
                jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(googleClientIds::contains)
                        ? OAuth2TokenValidatorResult.success() : failure("audience")));
        this.google = decoder;
    }

    public SocialProfile verify(AuthProvider provider, String idToken) {
        if (provider != AuthProvider.GOOGLE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER");
        }
        Jwt jwt;
        try {
            
            jwt = google.decode(idToken);
        } catch (BadJwtException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_SOCIAL_TOKEN");
        } catch (JwtException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SOCIAL_PROVIDER_UNAVAILABLE");
        }

        Object verified = jwt.getClaim("email_verified");
        return new SocialProfile(
                provider,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                verified instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(verified)),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("name"));
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
