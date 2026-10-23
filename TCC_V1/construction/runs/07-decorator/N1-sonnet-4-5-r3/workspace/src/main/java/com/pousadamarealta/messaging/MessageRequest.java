package com.pousadamarealta.messaging;

import com.pousadamarealta.domain.MessageChannel;

public class MessageRequest {
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;

    public MessageRequest(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public MessageRequest withTo(String newTo) {
        return new MessageRequest(channel, newTo, subject, body);
    }

    public MessageRequest withSubject(String newSubject) {
        return new MessageRequest(channel, to, newSubject, body);
    }

    public MessageRequest withBody(String newBody) {
        return new MessageRequest(channel, to, subject, newBody);
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
}
