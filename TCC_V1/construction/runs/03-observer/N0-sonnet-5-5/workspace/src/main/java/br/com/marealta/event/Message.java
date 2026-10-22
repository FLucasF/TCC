package br.com.marealta.event;

/** Mensagem enviada (por enquanto apenas registrada). */
public record Message(long id, Channel channel, String to, String subject, String body) {
    public enum Channel { EMAIL }

    public static Message email(String to, String subject, String body) {
        return new Message(0, Channel.EMAIL, to, subject, body);
    }
}
