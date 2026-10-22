package br.com.marealta.message;

/** Em que modo uma regra vale: sempre, só no modo de teste ou só fora dele. */
public enum RuleScope {
    ALWAYS, TEST_ONLY, LIVE_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
        };
    }
}
