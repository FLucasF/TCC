package br.com.marealta;

/** Quem recebe uma mensagem para enviar. As regras de envio são decoradores deste tipo. */
public interface MessageSender {
    void send(OutgoingMessage message);
}
