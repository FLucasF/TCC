package br.com.marealta;

import java.util.ArrayList;
import java.util.List;

/** Stand-in for a real mail sender: messages are only recorded. */
final class Outbox {
    private final List<Message> sent = new ArrayList<>();

    synchronized void sendEmail(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, "EMAIL", to, subject, body));
    }

    synchronized List<Message> all() {
        return List.copyOf(sent);
    }
}
