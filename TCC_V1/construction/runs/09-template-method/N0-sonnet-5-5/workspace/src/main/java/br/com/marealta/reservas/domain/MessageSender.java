package br.com.marealta.reservas.domain;

/** Porta de saída de mensagens ao hóspede; hoje só grava numa lista. */
public interface MessageSender {
    void send(String channel, String to, String subject, String body);
}
