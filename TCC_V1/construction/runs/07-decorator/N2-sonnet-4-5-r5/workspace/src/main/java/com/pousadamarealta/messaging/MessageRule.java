package com.pousadamarealta.messaging;

public interface MessageRule {
    void process(MessageContext context);
}
