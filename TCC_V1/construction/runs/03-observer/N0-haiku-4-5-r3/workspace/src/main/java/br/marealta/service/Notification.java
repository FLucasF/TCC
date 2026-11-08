package br.marealta.service;

import br.marealta.model.MessageChannel;

public class Notification {
    private final MessageChannel channel;
    private final String to;
    private final String subject;
    private final String body;

    public Notification(MessageChannel channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
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
