package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class ProcessedMessage {
    private final MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<ProcessedMessage> additionalMessages;

    public ProcessedMessage(PendingMessage pending) {
        this.channel = pending.channel();
        this.to = pending.to();
        this.subject = pending.subject();
        this.body = pending.body();
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
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

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public void addAdditionalMessage(ProcessedMessage message) {
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

    public List<ProcessedMessage> getAdditionalMessages() {
        return additionalMessages;
    }
}
