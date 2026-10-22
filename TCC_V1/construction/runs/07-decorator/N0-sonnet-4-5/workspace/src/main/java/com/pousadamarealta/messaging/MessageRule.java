package com.pousadamarealta.messaging;

public interface MessageRule {
    void apply(OutboundMessage message, RuleContext context);
}
