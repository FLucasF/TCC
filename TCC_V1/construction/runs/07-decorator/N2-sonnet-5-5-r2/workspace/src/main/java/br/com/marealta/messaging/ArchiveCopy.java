package br.com.marealta.messaging;

/** Toda mensagem enviada gera uma cópia para o arquivo, logo depois da original. */
public class ArchiveCopy implements MessageSender {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private final MessageSender next;

    public ArchiveCopy(MessageSender next) { this.next = next; }

    @Override
    public MessageStatus send(Message message) {
        MessageStatus status = next.send(message);
        if (status == MessageStatus.SENT) {
            next.send(message.withTo(ARCHIVE_ADDRESS).withSubject("[Cópia] " + message.subject()));
        }
        return status;
    }
}
