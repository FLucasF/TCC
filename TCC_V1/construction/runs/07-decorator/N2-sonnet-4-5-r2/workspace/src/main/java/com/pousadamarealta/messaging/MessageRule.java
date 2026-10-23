package com.pousadamarealta.messaging;

public interface MessageRule {
    boolean apply(OutgoingMessage message);
}
