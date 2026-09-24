package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;
import fr.kickoffmatchup.auth_server.model.User;
import fr.kickoffmatchup.auth_server.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NullMarked
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String nom = oAuth2User.getAttribute("name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(nom);
            newUser.setLastName("");
            newUser.setProvider(AuthProvider.GOOGLE);
            return userRepository.save(newUser);
        });

        return oAuth2User;
    }
}
