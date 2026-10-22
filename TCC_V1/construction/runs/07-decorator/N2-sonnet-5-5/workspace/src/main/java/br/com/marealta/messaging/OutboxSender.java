package br.com.marealta.messaging;

/** O envio de verdade (por enquanto): guardar na lista de mensagens enviadas. */
public final class OutboxSender implements MessageSender {
    private final MessageStore store;

    public OutboxSender(MessageStore store) { this.store = store; }

    @Override
    public Message send(Message message) {
        return store.save(message);
    }
}
