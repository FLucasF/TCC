package com.pousada.messaging;

import com.pousada.domain.Message;
import com.pousada.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class PolicyResult {
    private Message message;
    private boolean blocked;
    private final List<Message> additionalMessages;

    private PolicyResult(Message message, boolean blocked, List<Message> additionalMessages) {
        this.message = message;
        this.blocked = blocked;
        this.additionalMessages = additionalMessages;
    }

    public static PolicyResult pass(Message message) {
        return new PolicyResult(message, false, new ArrayList<>());
    }

    public static PolicyResult block(Message message) {
        Message blockedMessage = new Message(
            message.getChannel(),
            message.getTo(),
            message.getSubject(),
            message.getBody(),
            MessageStatus.BLOCKED
        );
        return new PolicyResult(blockedMessage, true, new ArrayList<>());
    }

    public void transform(Message newMessage) {
        if (!blocked) {
            this.message = newMessage;
        }
    }

    public void addMessage(Message message) {
        if (!blocked) {
            this.additionalMessages.add(message);
        }
    }

    public Message getMessage() {
        return message;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public List<Message> getAdditionalMessages() {
        return new ArrayList<>(additionalMessages);
    }

    public List<Message> getAllMessages() {
        List<Message> all = new ArrayList<>();
        all.add(message);
        all.addAll(additionalMessages);
        return all;
    }
}
