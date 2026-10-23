package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    private OutgoingMessage message;
    private final List<OutgoingMessage> additionalMessages;

    public MessageContext(OutgoingMessage message) {
        this.message = message;
        this.additionalMessages = new ArrayList<>();
    }

    public OutgoingMessage getMessage() {
        return message;
    }

    public void setMessage(OutgoingMessage message) {
        this.message = message;
    }

    public List<OutgoingMessage> getAdditionalMessages() {
        return additionalMessages;
    }

    public void addMessage(OutgoingMessage message) {
        additionalMessages.add(message);
    }

    public boolean isBlocked() {
        return message.getStatus() == com.pousadamarealta.domain.MessageStatus.BLOCKED;
    }
}
