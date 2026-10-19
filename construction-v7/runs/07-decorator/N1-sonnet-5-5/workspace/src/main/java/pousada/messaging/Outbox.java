package pousada.messaging;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens "enviadas" (por enquanto, só guardadas em memória). */
public class Outbox {
    private final MessagePipeline pipeline;
    private final List<Message> messages = new ArrayList<>();

    public Outbox(MessagePipeline pipeline) {
        this.pipeline = pipeline;
    }

    public synchronized void send(Message message) {
        for (Message m : pipeline.process(message)) {
            messages.add(m.withId(messages.size() + 1));
        }
    }

    public synchronized List<Message> all() {
        return List.copyOf(messages);
    }
}
