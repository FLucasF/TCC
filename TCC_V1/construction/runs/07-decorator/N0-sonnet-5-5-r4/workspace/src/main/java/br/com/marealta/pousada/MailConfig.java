package br.com.marealta.pousada;

import java.util.Locale;
import java.util.Map;

/**
 * Combinação de regras de e-mail de uma instalação. Cada regra opcional tem um escopo:
 * vale sempre, só no modo de teste, só fora dele, ou fica desligada.
 */
public record MailConfig(
        boolean testMode,
        Scope archiveCopy,
        Scope quietHours, int quietStartHour, int quietEndHour,
        Scope dailyLimit, int dailyLimitMax) {

    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    public enum Scope {
        ALWAYS, TEST_ONLY, LIVE_ONLY, OFF;

        public boolean active(boolean testMode) {
            return switch (this) {
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case LIVE_ONLY -> !testMode;
                case OFF -> false;
            };
        }

        static Scope parse(String raw, Scope def) {
            if (raw == null || raw.isBlank()) return def;
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "ALWAYS", "ON", "TRUE" -> ALWAYS;
                case "TEST", "TEST_ONLY" -> TEST_ONLY;
                case "LIVE", "LIVE_ONLY", "PROD", "PRODUCTION" -> LIVE_ONLY;
                case "OFF", "FALSE", "NEVER" -> OFF;
                default -> throw new IllegalArgumentException("Escopo inválido: " + raw);
            };
        }
    }

    /** Padrões: cópia para o arquivo fora do teste; horário de silêncio e limite desligados. */
    public static MailConfig defaults(boolean testMode) {
        return new MailConfig(testMode, Scope.LIVE_ONLY, Scope.OFF, 0, 6, Scope.OFF, 5);
    }

    public static MailConfig fromEnv(Map<String, String> env) {
        boolean test = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "").trim());
        MailConfig d = defaults(test);
        return new MailConfig(
                test,
                Scope.parse(env.get("HOTEL_MAIL_ARCHIVE_COPY"), d.archiveCopy),
                Scope.parse(env.get("HOTEL_MAIL_QUIET_HOURS"), d.quietHours),
                intOf(env.get("HOTEL_MAIL_QUIET_START"), d.quietStartHour),
                intOf(env.get("HOTEL_MAIL_QUIET_END"), d.quietEndHour),
                Scope.parse(env.get("HOTEL_MAIL_DAILY_LIMIT_SCOPE"), d.dailyLimit),
                intOf(env.get("HOTEL_MAIL_DAILY_LIMIT"), d.dailyLimitMax));
    }

    private static int intOf(String raw, int def) {
        return raw == null || raw.isBlank() ? def : Integer.parseInt(raw.trim());
    }
}
