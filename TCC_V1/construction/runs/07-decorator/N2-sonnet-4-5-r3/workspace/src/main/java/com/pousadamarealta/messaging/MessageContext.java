package com.pousadamarealta.messaging;

import java.util.ArrayList;
import java.util.List;

public final class MessageContext {
    private OutgoingMessage message;
    private MessageStatus status;
    private final List<OutgoingMessage> additionalMessages;

    public MessageContext(OutgoingMessage message) {
        this.message = message;
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
    }

    public OutgoingMessage getMessage() {
        return message;
    }

    public void setMessage(OutgoingMessage message) {
        this.message = message;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public void addAdditionalMessage(OutgoingMessage message) {
        additionalMessages.add(message);
    }

    public List<OutgoingMessage> getAdditionalMessages() {
        return List.copyOf(additionalMessages);
    }
}
