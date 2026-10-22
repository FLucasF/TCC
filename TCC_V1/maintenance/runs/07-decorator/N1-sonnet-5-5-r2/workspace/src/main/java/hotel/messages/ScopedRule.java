package hotel.messages;

import java.util.List;

/** Puts a rule in force only where its {@link Scope} says so. */
final class ScopedRule implements MessageRule {

    private final Scope scope;
    private final boolean testMode;
    private final MessageRule rule;

    ScopedRule(Scope scope, boolean testMode, MessageRule rule) {
        this.scope = scope;
        this.testMode = testMode;
        this.rule = rule;
    }

    @Override
    public Draft apply(Draft draft) {
        return scope.covers(testMode) ? rule.apply(draft) : draft;
    }

    @Override
    public List<Draft> extrasFor(Draft sent) {
        return scope.covers(testMode) ? rule.extrasFor(sent) : List.of();
    }
}
