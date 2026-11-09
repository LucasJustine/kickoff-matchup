package fr.kickoffmatchup.mail_service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


@Component
public class MailRenderer {

    private final TemplateEngine templates;
    private final MessageSource messages;
    private final String frontendUrl;

    public MailRenderer(TemplateEngine templates,
                        MessageSource messages,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.templates = templates;
        this.messages = messages;
        this.frontendUrl = frontendUrl;
    }

    public String message(String key, Locale locale, Object... args) {
        return messages.getMessage(key, args, locale);
    }

    public String render(String template, Locale locale, Map<String, ?> variables) {
        Context ctx = new Context(locale);
        ctx.setVariables(new HashMap<String, Object>(variables));
        return templates.process(template, ctx);
    }

    public String link(String path, String token) {
        return frontendUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    public String name(MailMessage message) {
        return message.firstName() == null ? "" : message.firstName();
    }
}
