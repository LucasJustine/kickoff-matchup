package fr.kickoffmatchup.auth_server.exception;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApi(ApiException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", e.getCode());
        body.put("message", e.getMessage() != null ? e.getMessage() : e.getCode()); // Pour avoir un texte lisible
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", "VALIDATION_ERROR");
        body.put("message", "Erreur de validation des champs");
        body.put("fields", fields);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleAmqp(AmqpException e) {
        log.error("Erreur lors de l'envoi d'un message AMQP : {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("success", false, "code", "MAIL_QUEUE_UNAVAILABLE",
                            "message", "Impossible d'envoyer l'email pour le moment."));
    }
}