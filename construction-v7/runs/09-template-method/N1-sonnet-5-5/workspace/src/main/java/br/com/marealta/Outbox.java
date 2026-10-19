package br.com.marealta;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens enviadas; por enquanto nada sai de verdade. */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized Message send(Channel channel, String to, String subject, String body) {
        Message message = new Message(sent.size() + 1, channel, to, subject, body);
        sent.add(message);
        return message;
    }

    public synchronized List<Message> all() {
        return List.copyOf(sent);
    }
}
