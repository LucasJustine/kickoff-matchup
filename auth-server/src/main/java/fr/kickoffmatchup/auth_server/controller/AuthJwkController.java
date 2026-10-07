package fr.kickoffmatchup.auth_server.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthJwkController {

    private final RSAKey rsaJwk;

    public AuthJwkController(RSAKey rsaJwk) {
        this.rsaJwk = rsaJwk;
    }

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> keys() {
        return new JWKSet(rsaJwk.toPublicJWK()).toJSONObject();
    }
}
