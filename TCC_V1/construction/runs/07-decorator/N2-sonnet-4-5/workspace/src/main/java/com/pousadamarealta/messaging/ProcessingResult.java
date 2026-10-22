package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public final class ProcessingResult {
    private final boolean blocked;
    private final OutgoingMessage processedMessage;
    private final List<OutgoingMessage> additionalMessages;

    private ProcessingResult(boolean blocked, OutgoingMessage processedMessage, List<OutgoingMessage> additionalMessages) {
        this.blocked = blocked;
        this.processedMessage = processedMessage;
        this.additionalMessages = additionalMessages;
    }

    public static ProcessingResult block(OutgoingMessage originalMessage) {
        return new ProcessingResult(true, originalMessage, List.of());
    }

    public static ProcessingResult proceed(OutgoingMessage processedMessage) {
        return new ProcessingResult(false, processedMessage, List.of());
    }

    public static ProcessingResult proceedWithAdditional(OutgoingMessage processedMessage, List<OutgoingMessage> additional) {
        return new ProcessingResult(false, processedMessage, new ArrayList<>(additional));
    }

    public boolean isBlocked() {
        return blocked;
    }

    public OutgoingMessage processedMessage() {
        return processedMessage;
    }

    public List<OutgoingMessage> additionalMessages() {
        return additionalMessages;
    }
}
