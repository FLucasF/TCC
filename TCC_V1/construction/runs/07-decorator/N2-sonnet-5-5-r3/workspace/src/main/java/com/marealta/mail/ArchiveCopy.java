package com.marealta.mail;

/** After a message is sent, sends a copy of it to the accountant's archive. */
public final class ArchiveCopy implements Outbox {
    private final Outbox next;
    private final String archiveAddress;

    public ArchiveCopy(Outbox next, String archiveAddress) {
        this.next = next;
        this.archiveAddress = archiveAddress;
    }

    @Override
    public Message send(Message message) {
        Message recorded = next.send(message);
        if (recorded.status() == MessageStatus.SENT) {
            next.send(Message.email(archiveAddress, "[Cópia] " + message.subject(), message.body()));
        }
        return recorded;
    }
}
