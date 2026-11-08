package br.com.marealta.events;

import br.com.marealta.domain.OutgoingMessage;
import br.com.marealta.service.MessageStore;

public final class StoredMessageSender implements MessageSender {
    private final MessageStore store;

    public StoredMessageSender(MessageStore store) {
        this.store = store;
    }

    @Override
    public void send(OutgoingMessage message) {
        store.save(message);
    }
}
