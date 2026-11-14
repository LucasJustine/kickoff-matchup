package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.dto.ApiDtos.AuthResult;
import fr.kickoffmatchup.auth_server.dto.ApiDtos.LoginResponse;
import fr.kickoffmatchup.auth_server.enums.LoginMethod;
import fr.kickoffmatchup.auth_server.exception.ApiException;
import fr.kickoffmatchup.auth_server.mail.EmailService;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import fr.kickoffmatchup.auth_server.util.EmailAddress;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MfaService mfa;
    private final EmailService emails;
    private final AuthTokenService tokens;

    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = encoder.encode(UUID.randomUUID().toString());
    }

    public LoginResponse login(String rawEmail, String password) {
        User user = authenticate(rawEmail, password);
        if (user.isMfaEnabled()) {
            MfaService.Challenge challenge = mfa.start(user.getId());
            emails.sendMfaCode(user.getEmail(), challenge.code());
            return LoginResponse.mfaRequired(challenge.id());
        }
        return LoginResponse.ok(tokens.issueFor(user, LoginMethod.PASSWORD));
    }

    public AuthResult verifyMfa(String challengeId, String code) {
        return tokens.issueFor(userOfChallenge(mfa.verify(challengeId, code)), LoginMethod.PASSWORD);
    }

    public void resendMfa(String challengeId) {
        MfaService.Resend resend = mfa.resend(challengeId);
        emails.sendMfaCode(userOfChallenge(resend.userId()).getEmail(), resend.code());
    }

    private User authenticate(String rawEmail, String password) {
        Optional<User> found = users.findByEmail(EmailAddress.normalize(rawEmail));

        String hash = found.map(User::getPassword).filter(Objects::nonNull).orElse(dummyHash);
        boolean passwordMatches = encoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
        }
        User user = found.get();
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }
        return user;
    }

    private User userOfChallenge(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "MFA_CHALLENGE_EXPIRED"));
    }
}
