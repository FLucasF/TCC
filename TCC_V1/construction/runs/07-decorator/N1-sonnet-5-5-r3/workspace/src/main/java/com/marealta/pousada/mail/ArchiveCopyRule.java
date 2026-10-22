package com.marealta.pousada.mail;

import java.util.List;

/** Acrescenta, logo depois da mensagem, uma cópia para o arquivo. */
public final class ArchiveCopyRule implements MailRule {
    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        Message copy = message.redirectedTo(archiveAddress, "[Cópia] " + message.subject(), message.body());
        return List.of(message, copy);
    }
}
