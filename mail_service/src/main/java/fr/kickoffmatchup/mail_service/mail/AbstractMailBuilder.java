package fr.kickoffmatchup.mail_service.mail;

import fr.kickoffmatchup.mail_contract.MailMessage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class AbstractMailBuilder<T extends MailMessage> implements MailBuilder<T> {

    protected final MailRenderer renderer;
    private final Class<T> type;
    private final String name;

    protected AbstractMailBuilder(MailRenderer renderer, Class<T> type, String name) {
        this.renderer = renderer;
        this.type = type;
        this.name = name;
    }

    protected abstract Map<String, Object> variables(T message);

    protected String subject(T message, Locale locale) {
        return renderer.message(name + ".subject", locale);
    }

    @Override
    public Class<T> messageType() {
        return type;
    }

    @Override
    public MailContent build(T message, Locale locale) {
        String subject = subject(message, locale);
        Map<String, Object> variables = new HashMap<>(variables(message));
        variables.put("subject", subject);
        return MailContent.html(subject, renderer.render("mail/" + name, locale, variables));
    }
}
