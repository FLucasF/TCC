package pousada.domain;

/** Entrega mensagens ao hóspede. Por enquanto elas só ficam guardadas. */
public interface Notifier {
    void send(Channel channel, String to, String subject, String body);
}
