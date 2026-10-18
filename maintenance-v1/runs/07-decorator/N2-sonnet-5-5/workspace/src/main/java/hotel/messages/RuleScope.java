package hotel.messages;

/** When a rule is in force: always, only in test mode, or only outside it. */
public enum RuleScope {
    ALWAYS,
    TEST_MODE_ONLY,
    NORMAL_MODE_ONLY;

    boolean appliesIn(boolean testMode) {
        return this == ALWAYS || (this == TEST_MODE_ONLY) == testMode;
    }
}
