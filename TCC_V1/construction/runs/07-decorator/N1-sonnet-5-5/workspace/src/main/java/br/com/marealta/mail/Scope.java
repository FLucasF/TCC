package br.com.marealta.mail;

/** Em que modo uma regra vale. */
public enum Scope {
    ALWAYS,
    TEST_ONLY,
    OUTSIDE_TEST_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case OUTSIDE_TEST_ONLY -> !testMode;
        };
    }
}
