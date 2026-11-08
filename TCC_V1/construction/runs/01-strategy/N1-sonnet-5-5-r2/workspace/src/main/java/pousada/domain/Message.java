package pousada.domain;

/** Mensagem enviada ao hóspede; {@code id} é nulo até ser guardada. */
public record Message(Long id, Channel channel, String to, String subject, String body) {
    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body);
    }
}
