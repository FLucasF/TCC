package br.com.marealta.messaging;

import java.util.List;

/** Lista de mensagens enviadas (e bloqueadas), que a recepção consulta. */
public interface MessageLog {
    void save(Message message, MessageStatus status);

    List<StoredMessage> all();
}
