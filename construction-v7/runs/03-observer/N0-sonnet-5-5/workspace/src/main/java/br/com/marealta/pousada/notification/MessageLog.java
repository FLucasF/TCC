package br.com.marealta.pousada.notification;

import java.util.ArrayList;
import java.util.List;

/** Stands in for a real mail sender: keeps what would have been sent. */
public class MessageLog {
    private final List<Message> messages = new ArrayList<>();

    public synchronized Message send(Channel channel, String to, String subject, String body) {
        Message message = new Message(messages.size() + 1L, channel, to, subject, body);
        messages.add(message);
        return message;
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
