package hotel.messages;

/** Where a rule applies: always, only in test mode, or only outside it. */
public enum RuleScope {
    ALWAYS,
    TEST_ONLY,
    NORMAL_ONLY;

    boolean applies(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case NORMAL_ONLY -> !testMode;
        };
    }

    static RuleScope parse(String name, RuleScope fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        return valueOf(name.trim().toUpperCase());
    }
}
