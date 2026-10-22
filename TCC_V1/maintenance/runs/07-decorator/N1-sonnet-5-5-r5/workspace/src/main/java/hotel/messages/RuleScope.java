package hotel.messages;

/** Where a rule is in force: always, only in test mode, or only outside it. */
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
