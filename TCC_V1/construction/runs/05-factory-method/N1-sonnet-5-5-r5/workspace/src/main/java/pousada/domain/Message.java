package pousada.domain;

/** Mensagem enviada ao hóspede; por enquanto só fica guardada. */
public record Message(long id, Channel channel, String to, String subject, String body) {
}
