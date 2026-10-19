package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

public class ProcessedMessage {
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public ProcessedMessage(MessageChannel channel, String to, String subject, String body, MessageStatus status) {
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
}
