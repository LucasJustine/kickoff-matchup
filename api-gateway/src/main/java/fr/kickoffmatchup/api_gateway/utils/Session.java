package fr.kickoffmatchup.api_gateway.utils;

import fr.kickoffmatchup.api_gateway.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class Session {

    private Session() {
    }

    public static void storeTokensInSession(HttpServletRequest request, String accessToken, String refreshToken) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            session = request.getSession(true);
        } else {
            request.changeSessionId();
        }
        if (accessToken != null) {
            session.setAttribute(SecurityConstants.SESSION_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            session.setAttribute(SecurityConstants.SESSION_REFRESH_TOKEN, refreshToken);
        }
    }
}
