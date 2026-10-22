package br.com.marealta;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

/** Configuração da instalação, lida das variáveis de ambiente. */
public record Config(
        int port,
        String dbUrl,
        String dbUser,
        String dbPassword,
        ZoneId zone,
        MessagePolicy.Config messages) {

    public static Config fromEnv(Map<String, String> env) {
        boolean test = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "").trim());
        String[] quiet = env.getOrDefault("HOTEL_QUIET_HOURS", "22:00-07:00").split("-");
        MessagePolicy.Config m = new MessagePolicy.Config(
                test,
                env.getOrDefault("HOTEL_TEST_ADDRESS", "testes@pousadamarealta.com.br"),
                env.getOrDefault("HOTEL_ARCHIVE_ADDRESS", "arquivo@pousadamarealta.com.br"),
                scope(env, "HOTEL_RULE_EMAIL_VALIDATION", MessagePolicy.Scope.ALWAYS),
                scope(env, "HOTEL_RULE_ARCHIVE_COPY", MessagePolicy.Scope.LIVE_ONLY),
                scope(env, "HOTEL_RULE_QUIET_HOURS", MessagePolicy.Scope.OFF),
                LocalTime.parse(quiet[0].trim()),
                LocalTime.parse(quiet[1].trim()),
                scope(env, "HOTEL_RULE_DAILY_LIMIT", MessagePolicy.Scope.OFF),
                Integer.parseInt(env.getOrDefault("HOTEL_DAILY_LIMIT", "3").trim()));
        return new Config(
                Integer.parseInt(env.getOrDefault("PORT", "7070").trim()),
                env.get("DB_URL"),
                env.get("DB_USER"),
                env.get("DB_PASSWORD"),
                ZoneId.of(env.getOrDefault("HOTEL_ZONE", "America/Sao_Paulo")),
                m);
    }

    private static MessagePolicy.Scope scope(Map<String, String> env, String key, MessagePolicy.Scope def) {
        String v = env.get(key);
        return v == null || v.isBlank() ? def : MessagePolicy.Scope.valueOf(v.trim().toUpperCase());
    }
}
