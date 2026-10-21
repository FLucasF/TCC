package br.com.pousadamarealta;

/** Where a delivery rule applies. */
public enum Scope {
    ALWAYS, TEST_ONLY, LIVE_ONLY, OFF;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
            case OFF -> false;
        };
    }
}
