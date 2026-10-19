package br.com.marealta.messaging;

public record Message(int id, Channel channel, String to, String subject, String body, MessageStatus status) {

    public static Message draft(Channel channel, String to, String subject, String body) {
        return new Message(0, channel, to, subject, body, MessageStatus.SENT);
    }

    public Message withId(int id) {
        return new Message(id, channel, to, subject, body, status);
    }

    public Message blocked() {
        return new Message(id, channel, to, subject, body, MessageStatus.BLOCKED);
    }

    public Message redirected(String newTo, String newSubject, String newBody) {
        return new Message(id, channel, newTo, newSubject, newBody, status);
    }

    public boolean isSent() { return status == MessageStatus.SENT; }
}
