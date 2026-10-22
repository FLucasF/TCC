package br.com.marealta.pousada.messaging;

import java.util.List;

/** Toda mensagem enviada gera, logo depois dela, uma cópia para o endereço de arquivo. */
public final class ArchiveCopyPolicy implements MessagePolicy {
    private final String archiveAddress;

    public ArchiveCopyPolicy(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        if (!message.isSent()) {
            return List.of(message);
        }
        Message copy = Message.email(archiveAddress, "[Cópia] " + message.subject(), message.body());
        return List.of(message, copy);
    }
}
