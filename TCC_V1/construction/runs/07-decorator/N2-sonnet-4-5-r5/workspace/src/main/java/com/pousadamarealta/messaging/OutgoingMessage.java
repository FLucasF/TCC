package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

public class OutgoingMessage {
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;
    private MessageStatus status;

    public OutgoingMessage(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
    }

    private OutgoingMessage(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
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

    public void block() {
        this.status = MessageStatus.BLOCKED;
    }

    public OutgoingMessage withModifications(String newTo, String newSubject, String newBody) {
        return new OutgoingMessage(this.channel, newTo, newSubject, newBody, this.status);
    }

    public OutgoingMessage copy(String newTo, String newSubject, String newBody) {
        return new OutgoingMessage(this.channel, newTo, newSubject, newBody);
    }
}
