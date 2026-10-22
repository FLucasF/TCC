package hotel.messages;

/** When a messaging rule applies. */
public enum RuleScope {
    OFF,
    ALWAYS,
    TEST_MODE_ONLY,
    NORMAL_MODE_ONLY;

    boolean applies(boolean testMode) {
        return switch (this) {
            case OFF -> false;
            case ALWAYS -> true;
            case TEST_MODE_ONLY -> testMode;
            case NORMAL_MODE_ONLY -> !testMode;
        };
    }
}
