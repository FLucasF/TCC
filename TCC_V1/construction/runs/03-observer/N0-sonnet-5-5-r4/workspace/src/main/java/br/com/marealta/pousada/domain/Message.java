package br.com.marealta.pousada.domain;

public record Message(Long id, Channel channel, String to, String subject, String body) {

    public enum Channel { EMAIL }

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body);
    }
}
