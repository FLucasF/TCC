package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageContext {
    private final Message message;
    private final List<Message> additionalMessages;
    private final String originalRecipient;

    public MessageContext(Message message) {
        this.message = message;
        this.additionalMessages = new ArrayList<>();
        this.originalRecipient = message.getTo();
    }

    public Message getMessage() {
        return message;
    }

    public List<Message> getAdditionalMessages() {
        return additionalMessages;
    }

    public void addMessage(Message additional) {
        additionalMessages.add(additional);
    }

    public String getOriginalRecipient() {
        return originalRecipient;
    }

    public List<Message> getAllMessages() {
        List<Message> all = new ArrayList<>();
        all.add(message);
        all.addAll(additionalMessages);
        return all;
    }
}
