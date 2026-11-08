package pousada.repo;

import pousada.domain.Channel;
import pousada.domain.Message;

import java.util.ArrayList;
import java.util.List;

public class InMemoryMessageRepository implements MessageRepository {
    private final List<Message> sent = new ArrayList<>();

    @Override
    public synchronized Message add(Channel channel, String to, String subject, String body) {
        Message m = new Message(sent.size() + 1, channel, to, subject, body);
        sent.add(m);
        return m;
    }

    @Override
    public synchronized List<Message> all() {
        return List.copyOf(sent);
    }
}
