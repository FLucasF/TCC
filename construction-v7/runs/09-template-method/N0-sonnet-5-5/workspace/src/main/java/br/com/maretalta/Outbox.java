package br.com.maretalta;

import java.util.ArrayList;
import java.util.List;

/** In-memory list of sent messages; stands in for a real e-mail gateway. */
public class Outbox {
    public enum Channel { EMAIL }

    public record Message(int id, Channel channel, String to, String subject, String body) {}

    private final List<Message> messages = new ArrayList<>();

    public synchronized void send(Channel channel, String to, String subject, String body) {
        messages.add(new Message(messages.size() + 1, channel, to, subject, body));
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
