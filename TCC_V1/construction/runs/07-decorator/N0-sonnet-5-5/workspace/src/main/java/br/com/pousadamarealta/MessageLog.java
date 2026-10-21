package br.com.pousadamarealta;

import java.util.ArrayList;
import java.util.List;

/** In-memory list of messages "sent" (no real delivery yet). */
public class MessageLog {
    private final List<Message> messages = new ArrayList<>();
    private long nextId = 1;

    public synchronized Message add(Message.Channel channel, String to, String subject, String body,
                                    Message.Status status) {
        Message m = new Message(nextId++, channel, to, subject, body, status);
        messages.add(m);
        return m;
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
