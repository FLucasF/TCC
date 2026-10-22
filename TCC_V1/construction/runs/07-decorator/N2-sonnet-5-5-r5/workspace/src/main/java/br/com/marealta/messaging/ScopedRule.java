package br.com.marealta.messaging;

public record ScopedRule(RuleScope scope, MessageRule rule) {

    public static ScopedRule always(MessageRule rule) {
        return new ScopedRule(RuleScope.ALWAYS, rule);
    }

    public static ScopedRule testOnly(MessageRule rule) {
        return new ScopedRule(RuleScope.TEST_ONLY, rule);
    }

    public static ScopedRule liveOnly(MessageRule rule) {
        return new ScopedRule(RuleScope.LIVE_ONLY, rule);
    }
}
