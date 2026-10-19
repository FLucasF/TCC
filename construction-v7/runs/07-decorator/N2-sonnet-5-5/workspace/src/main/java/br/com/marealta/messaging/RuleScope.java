package br.com.marealta.messaging;

/** Em que modo uma regra vale: sempre, só no modo de teste ou só fora dele. */
public enum RuleScope {
    ALWAYS, TEST_ONLY, NON_TEST_ONLY;

    public boolean appliesTo(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case NON_TEST_ONLY -> !testMode;
        };
    }
}
