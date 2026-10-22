package br.com.marealta.pousada;

import br.com.marealta.pousada.messaging.MessageRules;
import java.util.Map;

public record Config(int port, String dbUrl, String dbUser, String dbPassword, boolean testMode, String messageRules) {

    public static Config fromEnvironment(Map<String, String> env) {
        return new Config(
                Integer.parseInt(env.getOrDefault("PORT", "7070")),
                env.get("DB_URL"),
                env.get("DB_USER"),
                env.get("DB_PASSWORD"),
                "true".equalsIgnoreCase(env.get("HOTEL_TEST_MODE")),
                env.getOrDefault("HOTEL_MESSAGE_RULES", MessageRules.DEFAULT));
    }
}
