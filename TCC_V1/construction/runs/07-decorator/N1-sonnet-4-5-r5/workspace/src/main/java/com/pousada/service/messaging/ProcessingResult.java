package com.pousada.service.messaging;

import java.util.ArrayList;
import java.util.List;

public class ProcessingResult {
    private final List<OutgoingMessage> additionalMessages;

    public ProcessingResult() {
        this.additionalMessages = new ArrayList<>();
    }

    public void addMessage(OutgoingMessage message) {
        additionalMessages.add(message);
    }

    public List<OutgoingMessage> getAdditionalMessages() {
        return additionalMessages;
    }
}
