package br.com.marealta.pousada.messaging;

/** Em que modo do sistema uma regra vale. */
public enum Scope {
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
