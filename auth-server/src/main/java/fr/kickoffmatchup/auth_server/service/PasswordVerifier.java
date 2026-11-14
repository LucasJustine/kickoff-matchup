package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PasswordVerifier {

    private final PasswordEncoder encoder;

    public void requireMatch(User user, String currentPassword) {
        if (currentPassword == null || user.getPassword() == null
                || !encoder.matches(currentPassword, user.getPassword())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "INVALID_CURRENT_PASSWORD");
        }
    }
}
