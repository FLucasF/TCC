package br.com.marealta.service;

/** Saída de mensagens. Hoje só guarda numa lista; um canal real entra implementando isto. */
public interface MessageSender {
    void send(String channel, String to, String subject, String body);
}
