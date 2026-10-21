package br.com.marealta;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas (e bloqueadas); é o fim da cadeia de envio. */
public class Outbox implements MessageSender {
    private final List<SentMessage> messages = new ArrayList<>();

    @Override
    public synchronized void send(OutgoingMessage message) {
        store(message, MessageStatus.SENT);
    }

    public synchronized void block(OutgoingMessage message) {
        store(message, MessageStatus.BLOCKED);
    }

    public synchronized List<SentMessage> messages() {
        return List.copyOf(messages);
    }

    private void store(OutgoingMessage m, MessageStatus status) {
        messages.add(new SentMessage(messages.size() + 1, m.channel(), m.to(), m.subject(), m.body(), status));
    }
}
