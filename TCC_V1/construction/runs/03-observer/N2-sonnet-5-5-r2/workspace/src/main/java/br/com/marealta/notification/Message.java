package br.com.marealta.notification;

public record Message(long id, Channel channel, String to, String subject, String body) {

    public static Message email(String to, String subject, String body) {
        return new Message(0, Channel.EMAIL, to, subject, body);
    }
}
