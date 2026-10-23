package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import java.util.List;

/**
 * Regra de processamento de mensagens.
 * Cada regra pode modificar a mensagem original, criar novas mensagens ou bloquear o envio.
 */
public interface MessageRule {
    /**
     * Processa uma mensagem e retorna a lista de mensagens que devem ser enviadas.
     *
     * @param message Mensagem original
     * @return Lista de mensagens a enviar (pode estar vazia se bloqueada,
     *         pode ter a original modificada, ou pode ter mensagens adicionais)
     */
    List<Message> process(Message message);
}
