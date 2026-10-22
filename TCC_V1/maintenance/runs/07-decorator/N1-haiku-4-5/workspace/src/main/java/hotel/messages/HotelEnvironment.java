package hotel.messages;

import java.util.Map;

public final class HotelEnvironment {

    private final boolean testMode;

    public HotelEnvironment(Map<String, String> env) {
        this.testMode = "true".equalsIgnoreCase(env.getOrDefault("HOTEL_TEST_MODE", ""));
    }

    public boolean isTestMode() {
        return testMode;
    }
}
