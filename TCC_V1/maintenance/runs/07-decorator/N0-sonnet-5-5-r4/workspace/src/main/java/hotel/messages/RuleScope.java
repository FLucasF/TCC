package hotel.messages;

/** When a rule applies: always, only in test mode, or only outside of it. */
public enum RuleScope {
    ALWAYS,
    TEST_MODE_ONLY,
    NORMAL_MODE_ONLY;

    boolean covers(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_MODE_ONLY -> testMode;
            case NORMAL_MODE_ONLY -> !testMode;
        };
    }
}
