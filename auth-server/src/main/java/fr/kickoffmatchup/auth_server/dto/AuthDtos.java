package fr.kickoffmatchup.auth_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 72) String password) {}

    public record EmailRequest(@NotBlank @Email String email) {}

    public record TokenRequest(@NotBlank @Size(max = 200) String token) {}

    public record ResetPasswordRequest(
            @NotBlank @Size(max = 200) String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {}


    public record MfaToggleRequest(boolean enabled) {}


}
