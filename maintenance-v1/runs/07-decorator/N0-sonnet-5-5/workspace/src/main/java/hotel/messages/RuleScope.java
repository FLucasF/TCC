package hotel.messages;

/** Where a rule applies: in every mode, only in test mode, or only outside of it. */
public enum RuleScope {
    ALWAYS,
    TEST_ONLY,
    LIVE_ONLY;

    boolean appliesTo(boolean testMode) {
        return this == ALWAYS || (this == TEST_ONLY) == testMode;
    }
}
