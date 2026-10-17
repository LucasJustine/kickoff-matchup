package fr.kickoffmatchup.api_gateway.controller;

import fr.kickoffmatchup.api_gateway.security.SecurityConstants;
import fr.kickoffmatchup.api_gateway.utils.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/bff/auth")
public class BffAuthController {

    private final RestClient authServerClient;
    private static final Logger log = LoggerFactory.getLogger(BffAuthController.class);

    public BffAuthController(
            RestClient.Builder restClientBuilder,
            @Value("${auth-server.url:http://localhost:9000}") String authServerUrl) {
        this.authServerClient = restClientBuilder.baseUrl(authServerUrl).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> loginPayload, HttpServletRequest request) {
        LoginResponse response = authServerClient.post()
                .uri("/auth/login")
                .body(loginPayload)
                .retrieve()
                .body(LoginResponse.class);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if ("MFA_REQUIRED".equals(response.status())) {
            return ResponseEntity.ok(Map.of(
                    "status", "MFA_REQUIRED",
                    "challengeId", response.challengeId() != null ? response.challengeId() : ""
            ));
        }

        if (response.tokens() != null) {
            Session.storeTokensInSession(request, response.tokens().accessToken(), response.tokens().refreshToken());
        }

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "user", response.user() != null ? response.user() : Map.of()
        ));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, Object> mfaPayload, HttpServletRequest request) {
        AuthResult result = authServerClient.post()
                .uri("/auth/mfa/verify")
                .body(mfaPayload)
                .retrieve()
                .body(AuthResult.class);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (result.tokens() != null) {
            Session.storeTokensInSession(request, result.tokens().accessToken(), result.tokens().refreshToken());
        }

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "user", result.user() != null ? result.user() : Map.of()
        ));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session != null) {
            // 1. Récupération du Refresh Token stocké en session lors du login
            String refreshToken = (String) session.getAttribute("REFRESH_TOKEN");

            // 2. Notification à l'Auth Server pour révoquer le token
            if (refreshToken != null) {
                try {
                    authServerClient.post()
                            .uri("/auth/logout")
                            .body(new RefreshRequest(refreshToken))
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception e) {
                    log.warn("Échec de la notification de logout à l'Auth Server : {}", e.getMessage());
                }
            }

            // 3. Invalidation de la session Redis / Cookie de la Gateway
            session.invalidate();
        }

        // 4. Renvoi d'un statut 204 (No Content) à Angular
        return ResponseEntity.noContent().build();
    }



    private record RefreshRequest(String refreshToken) {}
    private record UserDto(Long id, String email, String firstName, String lastName, boolean mfaEnabled) {}
    private record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {}
    private record AuthResult(UserDto user, TokenPair tokens) {}
    private record LoginResponse(String status, UserDto user, TokenPair tokens, String challengeId) {}
}