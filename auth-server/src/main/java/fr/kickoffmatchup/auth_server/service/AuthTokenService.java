package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.dto.ApiDtos.AuthResult;
import fr.kickoffmatchup.auth_server.dto.ApiDtos.TokenPair;
import fr.kickoffmatchup.auth_server.dto.ApiDtos.UserDto;
import fr.kickoffmatchup.auth_server.jwt.JwtIssuer;
import fr.kickoffmatchup.auth_server.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtIssuer jwtIssuer;
    private final RefreshTokenService refreshTokens;

    public AuthResult issueFor(User user) {
        return result(user, refreshTokens.create(user));
    }

    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.Rotation rotation = refreshTokens.rotate(rawRefreshToken);
        return result(rotation.user(), rotation.refreshToken());
    }

    private AuthResult result(User user, String refreshToken) {
        TokenPair pair = new TokenPair(
                jwtIssuer.issue(user.getId(), user.getEmail()), refreshToken, "Bearer", jwtIssuer.ttlSeconds());
        return new AuthResult(UserDto.from(user), pair);
    }
}
