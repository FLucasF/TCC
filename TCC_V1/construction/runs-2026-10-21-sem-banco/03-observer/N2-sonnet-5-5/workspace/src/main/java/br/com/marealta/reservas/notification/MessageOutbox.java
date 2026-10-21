package br.com.marealta.reservas.notification;

import java.util.ArrayList;
import java.util.List;

/** Keeps the messages that would be sent; nothing leaves the system for now. */
public class MessageOutbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void email(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, Channel.EMAIL, to, subject, body));
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
