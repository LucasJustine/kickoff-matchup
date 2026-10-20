package fr.kickoffmatchup.api_gateway.config;

import java.net.URI;
import java.net.URISyntaxException;

public final class ReturnUrl {

    public static final String SESSION_ATTRIBUTE = "OAUTH_RETURN_URL";

    private ReturnUrl() {
    }

    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 2048) {
            return null;
        }

        if (!raw.startsWith("/") || raw.startsWith("//") || raw.contains("\\")) {
            return null;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || c == 0x7f) {
                return null;
            }
        }
        try {
            URI uri = new URI(raw);
            if (uri.getScheme() != null || uri.getAuthority() != null) {
                return null;
            }
        } catch (URISyntaxException e) {
            return null;
        }
        return raw;
    }
}
