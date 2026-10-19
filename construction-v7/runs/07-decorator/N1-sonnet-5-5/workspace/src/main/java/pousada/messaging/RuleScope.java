package pousada.messaging;

/** Em que modo de operação uma regra de mensagem vale. */
public enum RuleScope {
    ALWAYS, TEST_ONLY, LIVE_ONLY;

    public boolean appliesIn(boolean testMode) {
        return switch (this) {
            case ALWAYS -> true;
            case TEST_ONLY -> testMode;
            case LIVE_ONLY -> !testMode;
        };
    }
}
