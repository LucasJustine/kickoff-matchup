package fr.kickoffmatchup.auth_server.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender sender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String from;

    @Async
    public void sendVerification(String to, String firstName, String token) {
        String link = frontendUrl + "/verify-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        send(to, "Confirmez votre adresse email",
                "Bonjour " + firstName + ",\n\n"
                + "Pour activer votre compte, cliquez sur ce lien (valable 24 h) :\n" + link + "\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.");
    }

    @Async
    public void sendPasswordReset(String to, String firstName, String token) {
        String link = frontendUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        send(to, "Réinitialisation de votre mot de passe",
                "Bonjour " + firstName + ",\n\n"
                + "Pour choisir un nouveau mot de passe, cliquez sur ce lien (valable 1 h) :\n" + link + "\n\n"
                + "Si vous n'avez rien demandé, ignorez cet email : votre mot de passe reste inchangé.");
    }

    @Async
    public void sendMfaCode(String to, String code) {
        send(to, "Votre code de connexion : " + code,
                "Votre code de vérification est : " + code + "\n\n"
                + "Il est valable 5 minutes. Ne le communiquez à personne.\n"
                + "Si vous n'êtes pas à l'origine de cette connexion, changez votre mot de passe.");
    }

    @Async
    public void sendAccountAlreadyExists(String to, String firstName) {
        send(to, "Vous avez déjà un compte",
                "Bonjour " + firstName + ",\n\n"
                + "Quelqu'un vient d'essayer de créer un compte avec cette adresse, qui en a déjà un.\n"
                + "Connectez-vous normalement (avec Google si c'est ce que vous utilisez), ou utilisez "
                + "« Mot de passe oublié » pour définir un mot de passe.\n\n"
                + "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            sender.send(message);
        } catch (Exception e) {
            log.error("Échec d'envoi d'email ({})", subject, e);
        }
    }
}
