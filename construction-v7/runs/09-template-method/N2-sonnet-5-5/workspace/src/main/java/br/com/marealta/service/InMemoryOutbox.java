package br.com.marealta.service;

import java.util.ArrayList;
import java.util.List;

public class InMemoryOutbox implements MessageSender {
    private final List<Message> sent = new ArrayList<>();

    @Override
    public synchronized void send(String channel, String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, channel, to, subject, body));
    }

    public synchronized List<Message> all() {
        return List.copyOf(sent);
    }
}
