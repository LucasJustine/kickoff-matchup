package fr.kickoffmatchup.auth_server.service;

import fr.kickoffmatchup.auth_server.enums.AuthProvider;

public record SocialProfile(AuthProvider provider, String subject, String email, boolean emailVerified,
                            String givenName, String familyName, String name) {}
