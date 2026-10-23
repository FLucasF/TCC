package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.model.Message;
import java.util.ArrayList;
import java.util.List;

public class ProcessingResult {
    private final Message message;
    private final List<Message> additionalMessages;
    private final boolean shouldContinue;

    private ProcessingResult(Message message, List<Message> additionalMessages, boolean shouldContinue) {
        this.message = message;
        this.additionalMessages = additionalMessages;
        this.shouldContinue = shouldContinue;
    }

    public static ProcessingResult continueProcessing(Message message) {
        return new ProcessingResult(message, new ArrayList<>(), true);
    }

    public static ProcessingResult continueWithAdditional(Message message, Message additional) {
        List<Message> additionalMessages = new ArrayList<>();
        additionalMessages.add(additional);
        return new ProcessingResult(message, additionalMessages, true);
    }

    public static ProcessingResult block(Message message) {
        return new ProcessingResult(message, new ArrayList<>(), false);
    }

    public Message getMessage() {
        return message;
    }

    public List<Message> getAdditionalMessages() {
        return additionalMessages;
    }

    public boolean shouldContinue() {
        return shouldContinue;
    }
}
