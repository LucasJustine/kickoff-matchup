package fr.kickoffmatchup.mail_service.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@Component
public class MailFactory {

    private final Map<MailType, MailBuilder> builders = new EnumMap<>(MailType.class);
    private final Locale defaultLocale;

    public MailFactory(List<MailBuilder> all, @Value("${app.mail.default-locale}") String defaultLocale) {
        for (MailBuilder builder : all) {
            MailBuilder previous = builders.put(builder.type(), builder);
            if (previous != null) {
                throw new IllegalStateException("Deux builders pour le type " + builder.type() + " : "
                        + previous.getClass().getSimpleName() + " et " + builder.getClass().getSimpleName());
            }
        }
        for (MailType type : MailType.values()) {
            if (!builders.containsKey(type)) {
                throw new IllegalStateException("Aucun MailBuilder pour le type " + type);
            }
        }
        this.defaultLocale = Locale.forLanguageTag(defaultLocale);
    }

    public MailContent create(MailMessage message) {
        if (message.type() == null) {
            throw new IllegalArgumentException("Type de mail manquant");
        }
        return builders.get(message.type()).build(message, resolveLocale(message.locale()));
    }

    private Locale resolveLocale(String tag) {
        if (tag == null || tag.isBlank()) {
            return defaultLocale;
        }
        Locale locale = Locale.forLanguageTag(tag.replace('_', '-'));
        return locale.getLanguage().isEmpty() ? defaultLocale : locale;
    }
}
