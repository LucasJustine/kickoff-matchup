package fr.kickoffmatchup.mail_service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Component
public class MailFactory {

    private final Map<Class<?>, MailBuilder<?>> builders = new HashMap<>();
    private final Locale defaultLocale;

    public MailFactory(List<MailBuilder<?>> all, @Value("${app.mail.default-locale}") String defaultLocale) {
        for (MailBuilder<?> builder : all) {
            MailBuilder<?> previous = builders.put(builder.messageType(), builder);
            if (previous != null) {
                throw new IllegalStateException("Deux builders pour " + builder.messageType().getSimpleName() + " : "
                        + previous.getClass().getSimpleName() + " et " + builder.getClass().getSimpleName());
            }
        }
        for (Class<?> type : MailMessage.class.getPermittedSubclasses()) {
            if (!builders.containsKey(type)) {
                throw new IllegalStateException("Aucun MailBuilder pour " + type.getSimpleName());
            }
        }
        this.defaultLocale = Locale.forLanguageTag(defaultLocale);
    }

    @SuppressWarnings("unchecked")
    public MailContent create(MailMessage message) {
        MailBuilder<MailMessage> builder = (MailBuilder<MailMessage>) builders.get(message.getClass());
        return builder.build(message, resolveLocale(message.locale()));
    }

    private Locale resolveLocale(String tag) {
        if (tag == null || tag.isBlank()) {
            return defaultLocale;
        }
        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? defaultLocale : locale;
    }
}
