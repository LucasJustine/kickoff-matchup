package fr.kickoffmatchup.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;


@ConfigurationProperties("app.rate-limit")
public record RateLimitProperties(List<Rule> rules) {

    public RateLimitProperties {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    /**
     * @param path  Le pattern de chemin
     * @param capacity La capacité du seau à jetons
     * @param period La période de remplissage du seau à jetons
     */
    public record Rule(String path, int capacity, Duration period) {

        public Rule {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("app.rate-limit.rules : path obligatoire");
            }
            if (capacity < 1) {
                throw new IllegalArgumentException("app.rate-limit.rules[" + path + "] : capacity doit être >= 1");
            }
            if (period == null || period.toMillis() < 1) {
                throw new IllegalArgumentException("app.rate-limit.rules[" + path + "] : period doit être positive");
            }
        }
    }
}
