package fr.kickoffmatchup.auth_server.controller;


import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AuthProviderController {

    private final ClientRegistrationRepository clientRegistrationRepository;

    public AuthProviderController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/providers")
    public List<Map<String, String>> getProviders() {
        List<Map<String, String>> providers = new ArrayList<>();
        System.out.println("ClientRegistrationRepository class: " + clientRegistrationRepository.getClass().getName());
        if (clientRegistrationRepository instanceof Iterable<?>) {
            Iterable<ClientRegistration> registrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
            
            for (ClientRegistration registration : registrations) {
                Map<String, String> provider = new HashMap<>();
                provider.put("id", registration.getRegistrationId());
                provider.put("name", registration.getClientName()); 
            
                provider.put("url", "/oauth2/authorization/" + registration.getRegistrationId());
                
                providers.add(provider);
            }
        }
        return providers;
    }
}