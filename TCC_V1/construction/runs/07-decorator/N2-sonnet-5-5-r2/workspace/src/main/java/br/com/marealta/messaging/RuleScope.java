package br.com.marealta.messaging;

/** Onde uma regra de mensagens vale. */
public enum RuleScope {
    ALWAYS, TEST_MODE_ONLY, OUTSIDE_TEST_MODE_ONLY;

    boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_MODE_ONLY -> testMode;
            case OUTSIDE_TEST_MODE_ONLY -> !testMode;
        };
    }
}
