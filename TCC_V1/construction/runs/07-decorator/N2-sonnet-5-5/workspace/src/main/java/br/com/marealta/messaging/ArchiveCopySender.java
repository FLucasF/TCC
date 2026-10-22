package br.com.marealta.messaging;

/** Toda mensagem enviada gera, logo depois, uma cópia para o arquivo. Mensagem bloqueada não gera. */
public final class ArchiveCopySender implements MessageSender {
    private final String archiveAddress;
    private final MessageSender next;

    public ArchiveCopySender(String archiveAddress, MessageSender next) {
        this.archiveAddress = archiveAddress;
        this.next = next;
    }

    @Override
    public Message send(Message message) {
        Message sent = next.send(message);
        if (sent.status() == Message.Status.SENT) {
            next.send(Message.email(archiveAddress, "[Cópia] " + message.subject(), message.body()));
        }
        return sent;
    }
}
