package fr.kickoffmatchup.api_gateway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

public final class AuthServerDtos {

    private AuthServerDtos() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserDto(Long id, String email, String firstName, String lastName, boolean mfaEnabled,
                          boolean hasPassword, String loginMethod) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenPair(String accessToken, String refreshToken, String tokenType, long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthResult(UserDto user, TokenPair tokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginResponse(String status, UserDto user, TokenPair tokens, String challengeId) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(String code, String message, Map<String, String> fields) {
    }
}
