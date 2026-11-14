package fr.kickoffmatchup.auth_server.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * Spec « bff » (/v3/api-docs/bff) : uniquement les routes que le gateway expose à la SPA.
     * Exclut /auth/refresh et la connexion sociale, gérés côté gateway et jamais appelés par Angular.
     */
    @Bean
    GroupedOpenApi bffApi() {
        return GroupedOpenApi.builder()
            .group("bff")
            .pathsToMatch(
                "/auth/register",
                "/auth/verify-email",
                "/auth/resend-verification",
                "/auth/forgot-password",
                "/auth/reset-password",
                "/auth/login",
                "/auth/mfa/verify",
                "/auth/mfa/resend",
                "/auth/logout",
                "/auth/me",
                "/auth/me/mfa",
                "/auth/me/password",
                "/auth/me/password-link",
                "/auth/me/email",
                "/auth/confirm-email-change",
                "/auth/me/delete",
                "/auth/confirm-account-deletion")
            .build();
    }
}
