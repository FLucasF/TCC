package br.com.marealta;

public record Message(long id, Channel channel, String to, String subject, String body) {
    public enum Channel { EMAIL }

    /** Mensagem ainda não guardada (sem id). */
    public static Message email(String to, String subject, String body) {
        return new Message(0, Channel.EMAIL, to, subject, body);
    }
}
