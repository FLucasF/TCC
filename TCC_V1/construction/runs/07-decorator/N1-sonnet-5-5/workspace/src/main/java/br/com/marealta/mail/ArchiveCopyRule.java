package br.com.marealta.mail;

import java.util.List;

/** Acrescenta, logo depois da mensagem, uma cópia para o arquivo. */
public final class ArchiveCopyRule implements MailRule {
    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        Message copy = message.withTo(archiveAddress).withSubject("[Cópia] " + message.subject());
        return List.of(message, copy);
    }
}
