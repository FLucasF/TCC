package br.com.pousadamarealta.reservas.messaging;

public record ScopedRule(Scope scope, MessageRule rule) {

    public static ScopedRule always(MessageRule rule) {
        return new ScopedRule(Scope.ALWAYS, rule);
    }

    public static ScopedRule testOnly(MessageRule rule) {
        return new ScopedRule(Scope.TEST_ONLY, rule);
    }

    public static ScopedRule liveOnly(MessageRule rule) {
        return new ScopedRule(Scope.LIVE_ONLY, rule);
    }
}
