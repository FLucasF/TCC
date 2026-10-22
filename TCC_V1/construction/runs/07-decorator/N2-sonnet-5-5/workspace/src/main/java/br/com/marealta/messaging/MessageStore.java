package br.com.marealta.messaging;

import java.util.List;

/** A lista de mensagens enviadas. */
public interface MessageStore {
    Message save(Message message);

    List<Message> all();
}
