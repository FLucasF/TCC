package hotel.messages;

public final class EmailConfiguration {

    private final boolean testMode;
    private final boolean archiveCopy;

    public EmailConfiguration() {
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        this.archiveCopy = true;
    }

    public EmailConfiguration(boolean testMode, boolean archiveCopy) {
        this.testMode = testMode;
        this.archiveCopy = archiveCopy;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean shouldArchiveCopy() {
        return archiveCopy;
    }
}
