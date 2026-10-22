package br.com.marealta.messaging;

public record Message(Long id, Channel channel, String to, String subject, String body, MessageStatus status) {

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body, MessageStatus.SENT);
    }

    public Message blocked() {
        return new Message(id, channel, to, subject, body, MessageStatus.BLOCKED);
    }

    public Message with(String newTo, String newSubject, String newBody) {
        return new Message(id, channel, newTo, newSubject, newBody, status);
    }
}
