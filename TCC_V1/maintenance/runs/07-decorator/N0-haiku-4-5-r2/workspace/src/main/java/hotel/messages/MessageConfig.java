package hotel.messages;

import java.util.Map;

public final class MessageConfig {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final boolean testMode;
    private final boolean archiveEnabled;

    public MessageConfig() {
        this(Map.of());
    }

    public MessageConfig(Map<String, String> env) {
        this.testMode = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", "false"));
        this.archiveEnabled = !"false".equalsIgnoreCase(env.getOrDefault("HOTEL_ARCHIVE_ENABLED", "true"));
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean isArchiveEnabled() {
        return archiveEnabled;
    }

    public String getArchiveEmail() {
        return ARCHIVE_EMAIL;
    }

    public String getTestEmail() {
        return TEST_EMAIL;
    }
}
