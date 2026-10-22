package com.marealta.pousada.mail;

public enum Scope {
    ALWAYS, TEST_ONLY, NON_TEST_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case NON_TEST_ONLY -> !testMode;
        };
    }
}
