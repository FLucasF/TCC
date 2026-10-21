package hotel.messages;

public record ScopedRule(RuleScope scope, MessageRule rule) {

    public static ScopedRule always(MessageRule rule) {
        return new ScopedRule(RuleScope.ALWAYS, rule);
    }

    public static ScopedRule onlyInTestMode(MessageRule rule) {
        return new ScopedRule(RuleScope.TEST_MODE_ONLY, rule);
    }

    public static ScopedRule onlyOutsideTestMode(MessageRule rule) {
        return new ScopedRule(RuleScope.OUTSIDE_TEST_MODE_ONLY, rule);
    }
}
