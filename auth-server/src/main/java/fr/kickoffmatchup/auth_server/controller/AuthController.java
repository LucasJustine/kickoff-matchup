package fr.kickoffmatchup.auth_server.controller;

import fr.kickoffmatchup.auth_server.dto.ApiDtos.*;
import fr.kickoffmatchup.auth_server.dto.AuthDtos.*;
import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import fr.kickoffmatchup.auth_server.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService tokens;
    private final RefreshTokenService refreshTokens;
    private final MfaService mfaService;
    private final EmailService emailService;
    private final SocialTokenVerifier socialVerifier;
    private final OAuthUserService oauthUsers;
    private final UserRepository users;

    // ----- inscription, vérification email, mot de passe oublié -----
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest body) {
        authService.register(body);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest body) {
        authService.verifyEmail(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest body) {
        authService.resendVerification(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest body) {
        authService.forgotPassword(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        authService.resetPassword(body.token(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    // ----- connexion -----
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        User user = authService.authenticate(body.email(), body.password());
        if (user.isMfaEnabled()) {
            MfaService.Challenge challenge = mfaService.start(user.getId());
            emailService.sendMfaCode(user.getEmail(), challenge.code());
            return LoginResponse.mfaRequired(challenge.id());
        }
        return LoginResponse.ok(tokens.issueFor(user));
    }

    @PostMapping("/mfa/verify")
    public AuthResult verifyMfa(@Valid @RequestBody MfaVerifyRequest body) {
        Long userId = mfaService.verify(body.challengeId(), body.code());
        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED"));
        return tokens.issueFor(user);
    }

    /** Connexion sociale : le client présente l'ID token reçu du provider ; on le vérifie (signature + audience). */
    @PostMapping("/social/{provider}")
    public AuthResult social(@PathVariable String provider, @Valid @RequestBody SocialLoginRequest body) {
        SocialProfile profile = socialVerifier.verify(parseProvider(provider), body.idToken());
        return tokens.issueFor(oauthUsers.findOrCreate(profile));
    }

    // ----- cycle de vie des tokens -----
    @PostMapping("/refresh")
    public AuthResult refresh(@Valid @RequestBody RefreshRequest body) {
        return tokens.refresh(body.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest body) {
        refreshTokens.revokeFamilyOf(body.refreshToken());
        return ResponseEntity.noContent().build();
    }

    // ----- utilisateur courant (Bearer access token) -----
    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return UserDto.from(current(jwt));
    }

    /** Active/désactive le MFA par email. (À durcir : demander le mot de passe pour désactiver.) */
    @PutMapping("/me/mfa")
    public UserDto setMfa(@AuthenticationPrincipal Jwt jwt, @RequestBody MfaToggleRequest body) {
        User user = current(jwt);
        user.setMfaEnabled(body.enabled());
        return UserDto.from(users.save(user));
    }

    /** Récupère l'utilisateur courant à partir du token JWT. */
    private User current(Jwt jwt) {
        return users.findById(Long.valueOf(jwt.getSubject()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED"));
    }

    /** Convertit une chaîne en fournisseur d'authentification. */
    private static AuthProvider parseProvider(String value) {
        try {
            return AuthProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "UNSUPPORTED_PROVIDER");
        }
    }
}
