package br.com.pousadamarealta.reservas.messaging;

import java.util.ArrayList;
import java.util.List;

/** Ponto de saída das mensagens: aplica as regras e guarda o resultado na lista de mensagens. */
public final class Outbox {

    private final MessagePipeline pipeline;
    private final List<Message> messages = new ArrayList<>();

    public Outbox(MessagePipeline pipeline) {
        this.pipeline = pipeline;
    }

    public synchronized void send(Channel channel, String to, String subject, String body) {
        Draft draft = new Draft(channel, to, subject, body);
        pipeline.run(draft);
        record(draft, draft.isBlocked() ? MessageStatus.BLOCKED : MessageStatus.SENT);
        draft.copies().forEach(copy -> record(copy, MessageStatus.SENT));
    }

    public synchronized List<Message> messages() {
        return List.copyOf(messages);
    }

    private void record(Draft draft, MessageStatus status) {
        messages.add(new Message(messages.size() + 1L, draft.channel(), draft.to(), draft.subject(),
                draft.body(), status));
    }
}
