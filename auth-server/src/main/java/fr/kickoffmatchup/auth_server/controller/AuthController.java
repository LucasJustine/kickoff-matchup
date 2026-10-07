package fr.kickoffmatchup.auth_server.controller;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import fr.kickoffmatchup.auth_server.service.AuthTokenService;

@RestController
@RequestMapping
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            AuthTokenService authTokenService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.authTokenService = authTokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/auth/me")
    public UserResponse getMe(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));

        return new UserResponse(user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getProvider());
    }

    @PostMapping("/login")
    public LoginResponse login(
            @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.email(), loginRequest.password()));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            authTokenService.issue(authentication, request);

            return new LoginResponse("Connexion réussie");
        } catch (BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Identifiants invalides", exception);
        }
    }

    @PostMapping("/auth/refresh")
    public LoginResponse refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !authTokenService.hasRefreshToken(request)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalide");
        }

        authTokenService.issue(authentication, request);
        return new LoginResponse("Token renouvelé");
    }

    public record UserResponse(Long id, String email, String firstName,
                               String lastName, AuthProvider provider) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record LoginResponse(String message) {
    }
}
