package br.com.marealta.domain;

public record OutgoingMessage(Message.Channel channel, String to, String subject, String body) {
    public static OutgoingMessage email(String to, String subject, String body) {
        return new OutgoingMessage(Message.Channel.EMAIL, to, subject, body);
    }
}
