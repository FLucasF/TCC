package br.com.marealta.messaging;

/** Envia uma mensagem e diz se ela saiu (SENT) ou foi barrada (BLOCKED). */
public interface MessageSender {
    MessageStatus send(Message message);
}
