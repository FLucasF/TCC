package br.com.marealta.pousada.events;

import br.com.marealta.pousada.domain.Message;
import java.util.ArrayList;
import java.util.List;

/** Holds the messages "sent" so far (no real delivery yet). */
public class Outbox {
    private final List<Message> messages = new ArrayList<>();

    public synchronized Message sendEmail(String to, String subject, String body) {
        Message m = new Message(messages.size() + 1, Message.Channel.EMAIL, to, subject, body);
        messages.add(m);
        return m;
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
