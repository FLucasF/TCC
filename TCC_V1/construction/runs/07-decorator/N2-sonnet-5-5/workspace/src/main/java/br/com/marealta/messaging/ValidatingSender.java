package br.com.marealta.messaging;

/** Mensagem para endereço inválido não sai: fica na lista como BLOCKED, com o endereço original. */
public final class ValidatingSender implements MessageSender {
    private final MessageStore store;
    private final MessageSender next;

    public ValidatingSender(MessageStore store, MessageSender next) {
        this.store = store;
        this.next = next;
    }

    @Override
    public Message send(Message message) {
        if (!EmailAddress.isValid(message.to())) {
            return store.save(message.withStatus(Message.Status.BLOCKED));
        }
        return next.send(message);
    }
}
