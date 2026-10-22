package br.com.marealta.messaging;

/** Sends the message and, right after it, a copy to the archive address. */
public final class ArchiveCopy implements MessageSink {
    private static final String SUBJECT_PREFIX = "[Cópia] ";

    private final String archiveAddress;
    private final MessageSink next;

    public ArchiveCopy(String archiveAddress, MessageSink next) {
        this.archiveAddress = archiveAddress;
        this.next = next;
    }

    @Override
    public void deliver(Message message) {
        next.deliver(message);
        next.deliver(message.with(archiveAddress, SUBJECT_PREFIX + message.subject(), message.body()));
    }
}
