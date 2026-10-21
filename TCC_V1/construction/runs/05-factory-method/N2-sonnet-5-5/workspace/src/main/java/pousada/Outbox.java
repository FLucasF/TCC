package pousada;

import java.util.ArrayList;
import java.util.List;

/** Mensagens "enviadas": por enquanto ficam só nesta lista. */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void sendEmail(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, Message.Channel.EMAIL, to, subject, body));
    }

    public synchronized List<Message> all() { return List.copyOf(sent); }
}
