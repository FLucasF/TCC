package hotel.messages;

public interface TestModeProvider {
    boolean isTestMode();

    static TestModeProvider create() {
        return () -> "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }
}
