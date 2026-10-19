package br.com.mareal.domain;

import java.util.ArrayList;
import java.util.List;

/** Sent messages are only kept in memory for now. */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void sendEmail(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, "EMAIL", to, subject, body));
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
