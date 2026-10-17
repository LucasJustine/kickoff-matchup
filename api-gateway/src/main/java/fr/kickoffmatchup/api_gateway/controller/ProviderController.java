package fr.kickoffmatchup.api_gateway.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/providers")
public class ProviderController {
    
    private final ClientRegistrationRepository clientRegistrationRepository;

    public ProviderController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping()
    public List<ProviderDto> getProviders() {
        List<ProviderDto> providers = new ArrayList<>();

        if (clientRegistrationRepository instanceof InMemoryClientRegistrationRepository repository) {
            for (ClientRegistration registration : repository) {
                providers.add(new ProviderDto(
                        registration.getRegistrationId(),
                        registration.getClientName(),
                        "/oauth2/authorization/" + registration.getRegistrationId()
                ));
            }
        }

        return providers;
    }

    public record ProviderDto(String id, String name, String loginUrl) {}
}
