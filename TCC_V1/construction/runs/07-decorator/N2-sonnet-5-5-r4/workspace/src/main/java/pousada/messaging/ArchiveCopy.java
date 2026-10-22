package pousada.messaging;

/** Toda mensagem enviada gera, logo depois, uma cópia para o arquivo. */
public final class ArchiveCopy implements MessageSink {
    private final MessageSink next;
    private final String archiveAddress;

    public ArchiveCopy(MessageSink next, String archiveAddress) {
        this.next = next;
        this.archiveAddress = archiveAddress;
    }

    @Override
    public Message send(Message message) {
        Message result = next.send(message);
        if (result.status() == MessageStatus.SENT) {
            next.send(message.withTo(archiveAddress).withSubject("[Cópia] " + message.subject()));
        }
        return result;
    }
}
