package com.pousadamarealta.messaging;

public interface MessageRule {
    void apply(ProcessedMessage message);
}
