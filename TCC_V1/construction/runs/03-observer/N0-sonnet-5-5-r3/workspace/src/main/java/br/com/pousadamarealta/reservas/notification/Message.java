package br.com.pousadamarealta.reservas.notification;

/** Mensagem enviada. {@code id} é null até ser gravada. */
public record Message(Long id, Channel channel, String to, String subject, String body) {

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body);
    }
}
