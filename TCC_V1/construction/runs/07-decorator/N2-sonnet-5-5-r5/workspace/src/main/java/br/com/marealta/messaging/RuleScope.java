package br.com.marealta.messaging;

/** In which mode of the system a rule is part of the pipeline. */
public enum RuleScope {
    ALWAYS,
    TEST_ONLY,
    LIVE_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
        };
    }
}
