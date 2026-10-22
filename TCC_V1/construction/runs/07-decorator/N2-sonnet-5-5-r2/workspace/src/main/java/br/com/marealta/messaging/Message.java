package br.com.marealta.messaging;

public record Message(Channel channel, String to, String subject, String body) {
    public static Message email(String to, String subject, String body) {
        return new Message(Channel.EMAIL, to, subject, body);
    }

    public Message withTo(String newTo) { return new Message(channel, newTo, subject, body); }
    public Message withSubject(String newSubject) { return new Message(channel, to, newSubject, body); }
    public Message withBody(String newBody) { return new Message(channel, to, subject, newBody); }
}
