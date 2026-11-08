package com.pousada;

import com.pousada.domain.Message;
import com.pousada.repository.MessageRepository;
import java.util.ArrayList;
import java.util.List;

class InMemoryMessageRepository implements MessageRepository {
    private final List<Message> all = new ArrayList<>();

    @Override
    public synchronized Message save(Message m) {
        Message saved = new Message((long) all.size() + 1, m.channel(), m.to(), m.subject(), m.body());
        all.add(saved);
        return saved;
    }

    @Override
    public synchronized List<Message> findAll() {
        return List.copyOf(all);
    }
}
