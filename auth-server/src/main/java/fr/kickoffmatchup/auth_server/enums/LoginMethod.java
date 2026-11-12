package fr.kickoffmatchup.auth_server.enums;

public enum LoginMethod {
    PASSWORD,
    PROVIDER;

    public static LoginMethod fromClaim(String value) {
        return PROVIDER.name().equals(value) ? PROVIDER : PASSWORD;
    }
}
