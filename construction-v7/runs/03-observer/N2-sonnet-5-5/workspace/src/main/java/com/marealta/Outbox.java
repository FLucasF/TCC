package com.marealta;

import java.util.ArrayList;
import java.util.List;

/** Lista das mensagens "enviadas" (os envios ainda não saem de verdade). */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void send(Channel channel, String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, channel, to, subject, body));
    }

    public synchronized List<Message> all() {
        return List.copyOf(sent);
    }
}
