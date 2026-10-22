package hotel.messages;

import java.util.Map;

/** What each installation chooses about its e-mails. HOTEL_TEST_MODE=true turns test mode on. */
public record MessagingSettings(boolean testMode, boolean archiveCopy) {

    public static MessagingSettings normal() {
        return new MessagingSettings(false, true);
    }

    /** HOTEL_TEST_MODE (default false) and HOTEL_ARCHIVE_COPY (default true). */
    public static MessagingSettings from(Map<String, String> environment) {
        return new MessagingSettings(
                "true".equalsIgnoreCase(environment.get("HOTEL_TEST_MODE")),
                !"false".equalsIgnoreCase(environment.get("HOTEL_ARCHIVE_COPY")));
    }
}
