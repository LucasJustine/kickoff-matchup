package fr.kickoffmatchup.auth_server.controller;

import fr.kickoffmatchup.auth_server.dto.ApiDtos.*;
import fr.kickoffmatchup.auth_server.dto.AuthDtos.*;
import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.enums.LoginMethod;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.model.User;
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
    private final RegistrationService registration;
    private final PasswordService passwords;
    private final AccountService accounts;
    private final AuthTokenService tokens;
    private final RefreshTokenService refreshTokens;
    private final SocialTokenVerifier socialVerifier;
    private final OAuthUserService oauthUsers;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest body) {
        registration.register(body);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody TokenRequest body) {
        registration.verifyEmail(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody EmailRequest body) {
        registration.resendVerification(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody EmailRequest body) {
        passwords.forgotPassword(body.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        passwords.resetPassword(body.token(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body.email(), body.password());
    }

    @PostMapping("/mfa/verify")
    public AuthResult verifyMfa(@Valid @RequestBody MfaVerifyRequest body) {
        return authService.verifyMfa(body.challengeId(), body.code());
    }

    @PostMapping("/mfa/resend")
    public ResponseEntity<Void> resendMfa(@Valid @RequestBody MfaResendRequest body) {
        authService.resendMfa(body.challengeId());
        return ResponseEntity.accepted().build();
    }

    /** Connexion sociale : le client présente l'ID token reçu du provider ; on le vérifie (signature + audience). */
    @PostMapping("/social/{provider}")
    public AuthResult social(@PathVariable String provider, @Valid @RequestBody SocialLoginRequest body) {
        SocialProfile profile = socialVerifier.verify(parseProvider(provider), body.idToken());
        return tokens.issueFor(oauthUsers.findOrCreate(profile), LoginMethod.PROVIDER);
    }

    @PostMapping("/refresh")
    public AuthResult refresh(@Valid @RequestBody RefreshRequest body) {
        return tokens.refresh(body.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest body) {
        refreshTokens.revokeFamilyOf(body.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return UserDto.from(current(jwt), loginMethod(jwt));
    }

    @PutMapping("/me")
    public UserDto updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest body) {
        return UserDto.from(accounts.updateProfile(current(jwt), body.firstName(), body.lastName()), loginMethod(jwt));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest body) {
        passwords.changePassword(current(jwt), body.currentPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/password-link")
    public ResponseEntity<Void> requestPasswordLink(@AuthenticationPrincipal Jwt jwt) {
        passwords.requestPasswordLink(current(jwt));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/me/email")
    public ResponseEntity<Void> changeEmail(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody ChangeEmailRequest body) {
        accounts.requestEmailChange(current(jwt), body.newEmail(), body.currentPassword());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm-email-change")
    public ResponseEntity<Void> confirmEmailChange(@Valid @RequestBody TokenRequest body) {
        accounts.confirmEmailChange(body.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/delete")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody DeleteAccountRequest body) {
        boolean deleted = accounts.deleteAccount(current(jwt), body.currentPassword());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm-account-deletion")
    public ResponseEntity<Void> confirmAccountDeletion(@Valid @RequestBody TokenRequest body) {
        accounts.confirmAccountDeletion(body.token());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/mfa")
    public UserDto setMfa(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody MfaToggleRequest body) {
        LoginMethod loginMethod = loginMethod(jwt);
        return UserDto.from(accounts.setMfa(current(jwt), body.enabled(), body.currentPassword(), loginMethod), loginMethod);
    }

    private static LoginMethod loginMethod(Jwt jwt) {
        return LoginMethod.fromClaim(jwt.getClaimAsString("login_method"));
    }

    private User current(Jwt jwt) {
        return accounts.findById(Long.valueOf(jwt.getSubject()));
    }

    private static AuthProvider parseProvider(String value) {
        try {
            return AuthProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "UNSUPPORTED_PROVIDER");
        }
    }
}
