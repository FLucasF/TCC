package com.pousada.service.messaging;

public interface MessageRule {
    ProcessingResult process(OutgoingMessage message);
}
