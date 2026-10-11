package fr.kickoffmatchup.auth_server.dto;

import fr.kickoffmatchup.auth_server.enums.LoginStatus;
import fr.kickoffmatchup.auth_server.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ApiDtos {
    private ApiDtos() {}

    public record UserDto(Long id, String email, String firstName, String lastName, boolean mfaEnabled) {
        public static UserDto from(User u) {
            return new UserDto(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.isMfaEnabled());
        }
    }

    public record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    public record AuthResult(UserDto user, TokenPair tokens) {}

    public record LoginResponse(LoginStatus status, UserDto user, TokenPair tokens, String challengeId) {
        public static LoginResponse ok(AuthResult r) { return new LoginResponse(LoginStatus.OK, r.user(), r.tokens(), null); }
        public static LoginResponse mfaRequired(String challengeId) { return new LoginResponse(LoginStatus.MFA_REQUIRED, null, null, challengeId); }
    }

    public record MfaVerifyRequest(
            @NotBlank @Size(max = 100) String challengeId,
            @NotBlank @Pattern(regexp = "\\d{6}") String code) {}

    public record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {}

  
    public record SocialLoginRequest(@NotBlank @Size(max = 8000) String idToken) {}
}
