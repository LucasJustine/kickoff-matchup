package fr.kickoffmatchup.api_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class ReturnUrlCaptureFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_PREFIX = "/oauth2/authorization/";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(AUTHORIZATION_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String returnUrl = ReturnUrl.sanitize(request.getParameter("returnUrl"));
        HttpSession session = request.getSession(true);
        if (returnUrl != null) {
            session.setAttribute(ReturnUrl.SESSION_ATTRIBUTE, returnUrl);
        } else {
            session.removeAttribute(ReturnUrl.SESSION_ATTRIBUTE);
        }
        chain.doFilter(request, response);
    }
}
