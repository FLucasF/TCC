package br.com.marealta;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;

/** Configuração da instalação, lida das variáveis de ambiente. */
public record Config(
        int port, String dbUrl, String dbUser, String dbPassword,
        boolean testMode, ZoneId zone, Notifications notifications) {

    /** Em que modo uma regra de mensagem vale. */
    public enum Scope {
        ALWAYS, TEST_ONLY, LIVE_ONLY, NEVER;

        public boolean appliesTo(boolean testMode) {
            return switch (this) {
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case LIVE_ONLY -> !testMode;
                case NEVER -> false;
            };
        }
    }

    /** Regras das mensagens enviadas; cada instalação escolhe a sua combinação. */
    public record Notifications(
            Scope addressValidation, Scope archiveCopy, Scope quietHours, Scope dailyLimit,
            String archiveAddress, String testAddress,
            LocalTime quietStart, LocalTime quietEnd, int maxPerGuestPerDay) {

        public static Notifications defaults() {
            return new Notifications(Scope.ALWAYS, Scope.LIVE_ONLY, Scope.NEVER, Scope.NEVER,
                    "arquivo@pousadamarealta.com.br", "testes@pousadamarealta.com.br",
                    LocalTime.of(22, 0), LocalTime.of(7, 0), 5);
        }
    }

    public static Config fromEnv(Map<String, String> env) {
        Notifications d = Notifications.defaults();
        Notifications n = new Notifications(
                scope(env, "HOTEL_RULE_ADDRESS_VALIDATION", d.addressValidation()),
                scope(env, "HOTEL_RULE_ARCHIVE_COPY", d.archiveCopy()),
                scope(env, "HOTEL_RULE_QUIET_HOURS", d.quietHours()),
                scope(env, "HOTEL_RULE_DAILY_LIMIT", d.dailyLimit()),
                text(env, "HOTEL_ARCHIVE_ADDRESS", d.archiveAddress()),
                text(env, "HOTEL_TEST_ADDRESS", d.testAddress()),
                time(env, "HOTEL_QUIET_HOURS_START", d.quietStart()),
                time(env, "HOTEL_QUIET_HOURS_END", d.quietEnd()),
                Integer.parseInt(text(env, "HOTEL_DAILY_LIMIT", String.valueOf(d.maxPerGuestPerDay()))));
        return new Config(
                Integer.parseInt(text(env, "PORT", "7070")),
                env.get("DB_URL"), env.get("DB_USER"), env.get("DB_PASSWORD"),
                "true".equalsIgnoreCase(text(env, "HOTEL_TEST_MODE", "false")),
                ZoneId.of(text(env, "HOTEL_ZONE", "America/Sao_Paulo")),
                n);
    }

    private static String text(Map<String, String> env, String key, String def) {
        String v = env.get(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static Scope scope(Map<String, String> env, String key, Scope def) {
        return Scope.valueOf(text(env, key, def.name()).toUpperCase(Locale.ROOT));
    }

    private static LocalTime time(Map<String, String> env, String key, LocalTime def) {
        return LocalTime.parse(text(env, key, def.toString()));
    }
}
