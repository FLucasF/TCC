package br.com.pousadamarealta;

import java.time.ZoneId;
import java.util.Locale;
import java.util.function.Function;

/**
 * Per-installation settings. Address validation and the test-mode redirect are not configurable:
 * they are safety rules.
 */
public record AppConfig(boolean testMode,
                        Scope archiveCopy,
                        Scope quietHours, int quietStartHour, int quietEndHour,
                        Scope dailyLimit, int maxMessagesPerGuestPerDay,
                        ZoneId zone) {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    public static AppConfig defaults() {
        return new AppConfig(false, Scope.LIVE_ONLY, Scope.OFF, 0, 6, Scope.OFF, 5,
                ZoneId.of("America/Sao_Paulo"));
    }

    public AppConfig withTestMode(boolean value) {
        return new AppConfig(value, archiveCopy, quietHours, quietStartHour, quietEndHour,
                dailyLimit, maxMessagesPerGuestPerDay, zone);
    }

    public AppConfig withArchiveCopy(Scope value) {
        return new AppConfig(testMode, value, quietHours, quietStartHour, quietEndHour,
                dailyLimit, maxMessagesPerGuestPerDay, zone);
    }

    public AppConfig withQuietHours(Scope value, int startHour, int endHour) {
        return new AppConfig(testMode, archiveCopy, value, startHour, endHour,
                dailyLimit, maxMessagesPerGuestPerDay, zone);
    }

    public AppConfig withDailyLimit(Scope value, int max) {
        return new AppConfig(testMode, archiveCopy, quietHours, quietStartHour, quietEndHour,
                value, max, zone);
    }

    /**
     * Env vars: HOTEL_TEST_MODE, HOTEL_RULE_ARCHIVE_COPY, HOTEL_RULE_QUIET_HOURS (+ HOTEL_QUIET_START,
     * HOTEL_QUIET_END), HOTEL_RULE_DAILY_LIMIT (+ HOTEL_DAILY_LIMIT), HOTEL_TIMEZONE.
     * Rule values: ALWAYS, TEST_ONLY, LIVE_ONLY, OFF.
     */
    public static AppConfig fromEnv(Function<String, String> env) {
        AppConfig d = defaults();
        String tz = env.apply("HOTEL_TIMEZONE");
        return new AppConfig(
                "true".equalsIgnoreCase(trim(env.apply("HOTEL_TEST_MODE"))),
                scope(env.apply("HOTEL_RULE_ARCHIVE_COPY"), d.archiveCopy),
                scope(env.apply("HOTEL_RULE_QUIET_HOURS"), d.quietHours),
                integer(env.apply("HOTEL_QUIET_START"), d.quietStartHour),
                integer(env.apply("HOTEL_QUIET_END"), d.quietEndHour),
                scope(env.apply("HOTEL_RULE_DAILY_LIMIT"), d.dailyLimit),
                integer(env.apply("HOTEL_DAILY_LIMIT"), d.maxMessagesPerGuestPerDay),
                tz == null || tz.isBlank() ? d.zone : ZoneId.of(tz.trim()));
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static Scope scope(String value, Scope fallback) {
        return value == null || value.isBlank() ? fallback : Scope.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    private static int integer(String value, int fallback) {
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value.trim());
    }
}
