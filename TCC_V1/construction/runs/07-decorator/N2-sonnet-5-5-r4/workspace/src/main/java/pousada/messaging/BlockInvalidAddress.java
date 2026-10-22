package pousada.messaging;

/** Mensagem para endereço inválido não segue adiante: fica registrada como bloqueada. */
public final class BlockInvalidAddress implements MessageSink {
    private final MessageSink next;
    private final MessageSink blockedRecord;

    public BlockInvalidAddress(MessageSink next, MessageSink blockedRecord) {
        this.next = next;
        this.blockedRecord = blockedRecord;
    }

    @Override
    public Message send(Message message) {
        if (EmailAddress.isValid(message.to())) {
            return next.send(message);
        }
        return blockedRecord.send(message.blocked());
    }
}
