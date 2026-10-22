package br.com.marealta.messaging;

/**
 * Envia uma mensagem e devolve a mensagem como ficou registrada na lista de enviadas
 * (com id e situação SENT ou BLOCKED). As regras de envio são decoradores desta interface.
 */
public interface MessageSender {
    Message send(Message message);
}
