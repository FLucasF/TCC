package br.com.marealta.messaging;

/** Em que modo uma regra de envio vale. */
public enum Scope {
    ALWAYS, TEST_MODE_ONLY, OUTSIDE_TEST_MODE_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_MODE_ONLY -> testMode;
            case OUTSIDE_TEST_MODE_ONLY -> !testMode;
        };
    }
}
