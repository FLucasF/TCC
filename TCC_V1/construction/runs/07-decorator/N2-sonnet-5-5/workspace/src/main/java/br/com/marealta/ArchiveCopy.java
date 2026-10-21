package br.com.marealta;

/** Depois de enviar a mensagem, envia uma cópia para o arquivo do contador. */
public class ArchiveCopy implements MessageSender {
    private final MessageSender next;
    private final String archiveAddress;

    public ArchiveCopy(MessageSender next, String archiveAddress) {
        this.next = next;
        this.archiveAddress = archiveAddress;
    }

    @Override
    public void send(OutgoingMessage message) {
        next.send(message);
        next.send(message.withTo(archiveAddress).withSubject("[Cópia] " + message.subject()));
    }
}
