package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class ProcessingResult {
    private final OutgoingMessage message;
    private final boolean blocked;
    private final List<OutgoingMessage> additionalMessages;

    private ProcessingResult(OutgoingMessage message, boolean blocked, List<OutgoingMessage> additionalMessages) {
        this.message = message;
        this.blocked = blocked;
        this.additionalMessages = additionalMessages;
    }

    public static ProcessingResult allow(OutgoingMessage message) {
        return new ProcessingResult(message, false, new ArrayList<>());
    }

    public static ProcessingResult block(OutgoingMessage message) {
        return new ProcessingResult(message, true, new ArrayList<>());
    }

    public void addAdditionalMessage(OutgoingMessage message) {
        this.additionalMessages.add(message);
    }

    public OutgoingMessage getMessage() {
        return message;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<OutgoingMessage> getAdditionalMessages() {
        return additionalMessages;
    }
}
