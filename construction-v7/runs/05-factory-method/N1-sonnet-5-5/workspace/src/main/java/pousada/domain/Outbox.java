package pousada.domain;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens "enviadas": por enquanto nada sai de verdade. */
public class Outbox {

    private final List<Message> sent = new ArrayList<>();

    public synchronized Message send(Message.Channel channel, String to, String subject, String body) {
        Message message = new Message(sent.size() + 1L, channel, to, subject, body);
        sent.add(message);
        return message;
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
