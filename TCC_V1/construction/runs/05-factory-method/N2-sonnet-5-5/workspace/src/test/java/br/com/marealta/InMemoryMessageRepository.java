package br.com.marealta;

import java.util.ArrayList;
import java.util.List;

class InMemoryMessageRepository implements MessageRepository {
    private final List<Message> items = new ArrayList<>();

    public synchronized Message save(Message m) {
        Message saved = new Message(items.size() + 1L, m.channel(), m.to(), m.subject(), m.body());
        items.add(saved);
        return saved;
    }

    public synchronized List<Message> findAll() { return new ArrayList<>(items); }
}
