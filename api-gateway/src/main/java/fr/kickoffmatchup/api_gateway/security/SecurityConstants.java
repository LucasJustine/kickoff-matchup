package fr.kickoffmatchup.api_gateway.security;

public final class SecurityConstants {
    
    private SecurityConstants() {}

    public static final String SESSION_ACCESS_TOKEN = "ACCESS_TOKEN";

    public static final String SESSION_REFRESH_TOKEN = "REFRESH_TOKEN";

    public static final String REQUEST_ACCESS_TOKEN_ATTRIBUTE = "fr.kickoffmatchup.api_gateway.jwt.ACCESS_TOKEN";
}