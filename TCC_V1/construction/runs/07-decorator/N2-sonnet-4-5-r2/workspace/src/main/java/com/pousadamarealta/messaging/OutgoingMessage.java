package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public final class OutgoingMessage {
    private final MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<OutgoingMessage> additionalMessages;

    public OutgoingMessage(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
    }

    public void block() {
        this.status = MessageStatus.BLOCKED;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void addAdditionalMessage(OutgoingMessage message) {
        this.additionalMessages.add(message);
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public List<OutgoingMessage> getAdditionalMessages() {
        return new ArrayList<>(additionalMessages);
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }
}
