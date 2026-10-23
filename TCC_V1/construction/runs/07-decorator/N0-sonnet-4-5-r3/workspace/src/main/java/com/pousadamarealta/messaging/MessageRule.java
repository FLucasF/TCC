package com.pousadamarealta.messaging;

public interface MessageRule {
    ProcessingResult process(OutgoingMessage message);
}
