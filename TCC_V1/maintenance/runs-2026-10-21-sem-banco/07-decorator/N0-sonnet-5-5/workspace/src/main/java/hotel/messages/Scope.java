package hotel.messages;

/** When a rule applies: always, only in test mode, or only outside it. */
public enum Scope {
    ALWAYS,
    TEST_ONLY,
    NORMAL_ONLY;

    boolean applies(boolean testMode) {
        return this == ALWAYS || (this == TEST_ONLY) == testMode;
    }
}
