package com.pousadamarealta.messaging;

import com.pousadamarealta.model.MessageChannel;

public class OutgoingMessage {
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;

    public OutgoingMessage(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public MessageChannel getChannel() {
        return channel;
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public OutgoingMessage copy() {
        return new OutgoingMessage(channel, to, subject, body);
    }
}
