package hotel.messages;

import java.util.List;

/** A rule that only counts in the test mode, only outside it, or always. */
public final class ScopedRule implements MessageRule {

    public enum Scope {
        ALWAYS,
        TEST_MODE_ONLY,
        NORMAL_MODE_ONLY
    }

    private final MessageRule rule;
    private final boolean active;

    private ScopedRule(MessageRule rule, Scope scope, boolean testMode) {
        this.rule = rule;
        this.active = switch (scope) {
            case ALWAYS -> true;
            case TEST_MODE_ONLY -> testMode;
            case NORMAL_MODE_ONLY -> !testMode;
        };
    }

    public static MessageRule of(MessageRule rule, Scope scope, boolean testMode) {
        return new ScopedRule(rule, scope, testMode);
    }

    @Override
    public List<Draft> apply(Draft draft) {
        return active ? rule.apply(draft) : List.of(draft);
    }
}
