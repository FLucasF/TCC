package hotel.messages;

public class MessageRules {

    private final boolean testMode;

    public MessageRules() {
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }

    public boolean isTestMode() {
        return testMode;
    }
}
