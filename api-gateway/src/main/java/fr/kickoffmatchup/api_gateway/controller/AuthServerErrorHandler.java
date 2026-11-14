package fr.kickoffmatchup.api_gateway.controller;

import fr.kickoffmatchup.api_gateway.dto.AuthServerDtos.ErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AuthServerErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthServerErrorHandler.class);

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<?> handleClientError(HttpClientErrorException e, HttpServletRequest request) {
        if (e.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                && "/bff/auth/login".equals(request.getRequestURI())) {
            return json(e.getStatusCode(), Map.of(
                    "success", false,
                    "code", "INVALID_CREDENTIALS",
                    "message", "Email ou mot de passe incorrect."));
        }
        ErrorBody body = parse(e);
        if (body != null && body.code() != null) {
            Map<String, Object> response = error(body.code(), body.message() != null ? body.message() : body.code());
            if (body.fields() != null) {
                response.put("fields", body.fields());
            }
            return json(e.getStatusCode(), response);
        }
        return json(e.getStatusCode(), error("AUTH_SERVER_ERROR", "Requête refusée par le service d'authentification."));
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<?> handleServerError(HttpServerErrorException e) {
        log.warn("L'auth server a répondu HTTP {}", e.getStatusCode().value());
        return json(HttpStatus.BAD_GATEWAY,
                error("AUTH_SERVER_ERROR", "Le service d'authentification a rencontré une erreur."));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<?> handleUnavailable(ResourceAccessException e) {
        log.warn("Auth server injoignable : {}", e.getMessage());
        return json(HttpStatus.SERVICE_UNAVAILABLE,
                error("AUTH_SERVER_UNAVAILABLE", "Le service d'authentification est indisponible."));
    }

    private static ErrorBody parse(HttpClientErrorException e) {
        try {
            return e.getResponseBodyAs(ErrorBody.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    private static ResponseEntity<Map<String, ?>> json(HttpStatusCode status, Map<String, ?> body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
