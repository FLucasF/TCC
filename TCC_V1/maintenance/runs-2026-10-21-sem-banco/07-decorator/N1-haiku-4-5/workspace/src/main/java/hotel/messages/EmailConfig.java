package hotel.messages;

public final class EmailConfig {
    private final boolean testMode;
    private final String archiveEmail;

    public EmailConfig(boolean testMode, String archiveEmail) {
        this.testMode = testMode;
        this.archiveEmail = archiveEmail;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public String archiveEmail() {
        return archiveEmail;
    }

    public static EmailConfig production() {
        return new EmailConfig(false, null);
    }

    public static EmailConfig fromEnvironment() {
        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        String archiveEmail = System.getenv("HOTEL_ARCHIVE_EMAIL");
        return new EmailConfig(testMode, archiveEmail);
    }
}
