package hotel.messages;

/** Where a rule is in force: always, only in test mode, or only outside of it. */
public enum Scope {
    ALWAYS,
    TEST_ONLY,
    LIVE_ONLY;

    boolean appliesIn(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
        };
    }
}
