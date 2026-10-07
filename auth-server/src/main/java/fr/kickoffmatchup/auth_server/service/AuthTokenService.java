package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthTokenService {

    static final String ACCESS_TOKEN_SESSION_ATTRIBUTE = "ACCESS_TOKEN";
    static final String REFRESH_TOKEN_SESSION_ATTRIBUTE = "REFRESH_TOKEN";

    private final JwtEncoder jwtEncoder;
    private final long accessTokenExpiration;
    private final UserRepository userRepository;

    public AuthTokenService(
            UserRepository userRepository,
            JwtEncoder jwtEncoder,
            @Value("${jwt.expiration:3600000}") long accessTokenExpiration) {
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public void issue(Authentication authentication, HttpServletRequest request) {
        String accessToken = createAccessToken(authentication);
        String refreshToken = UUID.randomUUID().toString();

        var session = request.getSession(true);
        session.setAttribute(ACCESS_TOKEN_SESSION_ATTRIBUTE, accessToken);
        session.setAttribute(REFRESH_TOKEN_SESSION_ATTRIBUTE, refreshToken);
    }

    public boolean hasRefreshToken(HttpServletRequest request) {
        var session = request.getSession(false);
        return session != null
                && session.getAttribute(REFRESH_TOKEN_SESSION_ATTRIBUTE) != null;
    }

    private String createAccessToken(Authentication authentication) {
        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Utilisateur introuvable : " + authentication.getName()));

        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusMillis(accessTokenExpiration))
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId("auth-server-key")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }

}
