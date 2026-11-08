package br.com.marealta.pousada.notification;

/** Mensagem enviada; o id fica nulo até ela ser guardada. */
public record Message(Long id, Channel channel, String to, String subject, String body) {

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body);
    }
}
