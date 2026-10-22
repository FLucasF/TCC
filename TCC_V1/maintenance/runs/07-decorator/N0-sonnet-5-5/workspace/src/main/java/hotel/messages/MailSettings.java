package hotel.messages;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * What one installation of the system wants from its e-mails. Environment variables:
 * <ul>
 *   <li>HOTEL_TEST_MODE=true: every message goes to the test address, nothing is archived.</li>
 *   <li>HOTEL_ARCHIVE_COPY=false: no copy to the archive (default: copy).</li>
 *   <li>HOTEL_QUIET_HOURS=22:00-07:00: no messages in that period (default: none), with
 *       HOTEL_QUIET_HOURS_SCOPE=ALWAYS|TEST_ONLY|NORMAL_ONLY (default ALWAYS).</li>
 *   <li>HOTEL_DAILY_LIMIT=3: most messages a guest gets in a day (default: no limit), with
 *       HOTEL_DAILY_LIMIT_SCOPE (default ALWAYS).</li>
 *   <li>HOTEL_ZONE: time zone of the quiet hours and of the "day" (default America/Sao_Paulo).</li>
 * </ul>
 */
public record MailSettings(
        boolean testMode,
        boolean archiveCopy,
        LocalTime quietFrom,
        LocalTime quietUntil,
        RuleScope quietScope,
        Integer dailyLimit,
        RuleScope limitScope,
        ZoneId zone) {

    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    /** Normal mode, with the archive copy and no other rule. */
    public static MailSettings defaults() {
        return fromEnvironment(Map.of());
    }

    public static MailSettings fromEnvironment(Map<String, String> environment) {
        boolean testMode = "true".equalsIgnoreCase(environment.getOrDefault("HOTEL_TEST_MODE", "").trim());
        boolean archive = !"false".equalsIgnoreCase(environment.getOrDefault("HOTEL_ARCHIVE_COPY", "").trim());
        LocalTime from = null;
        LocalTime until = null;
        String quiet = environment.get("HOTEL_QUIET_HOURS");
        if (quiet != null && !quiet.isBlank()) {
            String[] parts = quiet.trim().split("-");
            if (parts.length != 2) {
                throw new IllegalStateException("HOTEL_QUIET_HOURS must look like 22:00-07:00");
            }
            from = LocalTime.parse(parts[0].trim());
            until = LocalTime.parse(parts[1].trim());
        }
        Integer limit = null;
        String limitText = environment.get("HOTEL_DAILY_LIMIT");
        if (limitText != null && !limitText.isBlank()) {
            limit = Integer.valueOf(limitText.trim());
        }
        String zone = environment.get("HOTEL_ZONE");
        return new MailSettings(
                testMode,
                archive,
                from,
                until,
                RuleScope.parse(environment.get("HOTEL_QUIET_HOURS_SCOPE"), RuleScope.ALWAYS),
                limit,
                RuleScope.parse(environment.get("HOTEL_DAILY_LIMIT_SCOPE"), RuleScope.ALWAYS),
                zone == null || zone.isBlank() ? ZoneId.of("America/Sao_Paulo") : ZoneId.of(zone.trim()));
    }

}
