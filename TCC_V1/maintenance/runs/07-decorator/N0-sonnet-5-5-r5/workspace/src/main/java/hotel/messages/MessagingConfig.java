package hotel.messages;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Which rules one installation of the system applies to the e-mails. In test mode no message ever goes to the real
 * address (that is not optional); the other rules each have a {@link RuleScope}.
 *
 * <p>Environment: HOTEL_TEST_MODE=true; HOTEL_RULE_ADDRESS_CHECK, HOTEL_RULE_ARCHIVE_COPY, HOTEL_RULE_QUIET_HOURS and
 * HOTEL_RULE_DAILY_LIMIT, each OFF, ALWAYS, TEST_MODE_ONLY or NORMAL_MODE_ONLY; HOTEL_QUIET_FROM / HOTEL_QUIET_TO
 * (HH:mm), HOTEL_DAILY_LIMIT (messages per guest per day) and HOTEL_TIMEZONE.
 */
public record MessagingConfig(
        boolean testMode,
        RuleScope addressCheck,
        RuleScope archiveCopy,
        RuleScope quietHours,
        LocalTime quietFrom,
        LocalTime quietTo,
        RuleScope dailyLimit,
        int maxPerGuestPerDay,
        ZoneId zone) {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    /** Normal mode, address check and archive copy on; quiet hours and daily limit off until an installation asks. */
    public static MessagingConfig defaults() {
        return fromEnvironment(Map.of());
    }

    public MessagingConfig withTestMode(boolean value) {
        return new MessagingConfig(value, addressCheck, archiveCopy, quietHours, quietFrom, quietTo, dailyLimit,
                maxPerGuestPerDay, zone);
    }

    public MessagingConfig withArchiveCopy(RuleScope scope) {
        return new MessagingConfig(testMode, addressCheck, scope, quietHours, quietFrom, quietTo, dailyLimit,
                maxPerGuestPerDay, zone);
    }

    public MessagingConfig withQuietHours(RuleScope scope, LocalTime from, LocalTime to) {
        return new MessagingConfig(testMode, addressCheck, archiveCopy, scope, from, to, dailyLimit,
                maxPerGuestPerDay, zone);
    }

    public MessagingConfig withDailyLimit(RuleScope scope, int max) {
        return new MessagingConfig(testMode, addressCheck, archiveCopy, quietHours, quietFrom, quietTo, scope, max,
                zone);
    }

    public static MessagingConfig fromEnvironment(Map<String, String> env) {
        return new MessagingConfig(
                "true".equalsIgnoreCase(value(env, "HOTEL_TEST_MODE", "false")),
                scope(env, "HOTEL_RULE_ADDRESS_CHECK", RuleScope.ALWAYS),
                scope(env, "HOTEL_RULE_ARCHIVE_COPY", RuleScope.NORMAL_MODE_ONLY),
                scope(env, "HOTEL_RULE_QUIET_HOURS", RuleScope.OFF),
                LocalTime.parse(value(env, "HOTEL_QUIET_FROM", "22:00")),
                LocalTime.parse(value(env, "HOTEL_QUIET_TO", "07:00")),
                scope(env, "HOTEL_RULE_DAILY_LIMIT", RuleScope.OFF),
                Integer.parseInt(value(env, "HOTEL_DAILY_LIMIT", "5")),
                ZoneId.of(value(env, "HOTEL_TIMEZONE", "America/Sao_Paulo")));
    }

    private static String value(Map<String, String> env, String name, String fallback) {
        String value = env.get(name);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static RuleScope scope(Map<String, String> env, String name, RuleScope fallback) {
        return RuleScope.valueOf(value(env, name, fallback.name()).toUpperCase());
    }
}
