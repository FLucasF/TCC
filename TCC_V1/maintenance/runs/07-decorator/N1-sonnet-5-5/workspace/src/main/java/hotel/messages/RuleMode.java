package hotel.messages;

/** When a rule is in force: always, only in test mode, or only outside it. */
public enum RuleMode {
    ALWAYS,
    ONLY_IN_TEST,
    ONLY_OUTSIDE_TEST;

    boolean applies(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case ONLY_IN_TEST -> testMode;
            case ONLY_OUTSIDE_TEST -> !testMode;
        };
    }
}
