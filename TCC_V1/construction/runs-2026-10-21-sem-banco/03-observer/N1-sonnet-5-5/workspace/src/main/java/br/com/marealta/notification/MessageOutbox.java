package br.com.marealta.notification;

import java.util.ArrayList;
import java.util.List;

/** Guarda as mensagens "enviadas" na memória, até existir um envio de verdade. */
public class MessageOutbox implements MessageSender {
    private final List<Message> sent = new ArrayList<>();

    @Override
    public synchronized void send(Channel channel, String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1L, channel, to, subject, body));
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
