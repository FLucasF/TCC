package br.com.marealta.message;

public record Message(Long id, Channel channel, String to, String subject, String body, MessageStatus status) {

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body, MessageStatus.SENT);
    }
}
