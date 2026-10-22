package br.com.marealta.pousada.mail;

/** Onde uma regra vale: sempre, só no modo de teste ou só fora dele. */
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
