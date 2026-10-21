package pousada;

import java.util.ArrayList;
import java.util.List;

/** Sent messages; nothing leaves the system for now. */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void sendEmail(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, Message.Channel.EMAIL, to, subject, body));
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
