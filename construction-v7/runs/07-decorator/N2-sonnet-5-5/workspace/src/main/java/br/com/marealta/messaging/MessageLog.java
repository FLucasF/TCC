package br.com.marealta.messaging;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas (e bloqueadas), em memória. */
public class MessageLog {
    private final List<Message> messages = new ArrayList<>();

    public synchronized void addAll(List<Message> newMessages) {
        for (Message message : newMessages) {
            messages.add(message.withId(messages.size() + 1));
        }
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
