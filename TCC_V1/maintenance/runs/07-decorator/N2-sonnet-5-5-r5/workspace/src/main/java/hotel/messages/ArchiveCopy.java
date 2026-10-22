package hotel.messages;

/** Every message sent also goes, as a copy, to the address the accountant reads. Blocked messages have no copy. */
public final class ArchiveCopy implements MessageSender {

    private final MessageSender next;
    private final String archiveAddress;

    public ArchiveCopy(MessageSender next, String archiveAddress) {
        this.next = next;
        this.archiveAddress = archiveAddress;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message original = next.send(to, subject, body);
        if (original.status() == MessageStatus.SENT) {
            next.send(archiveAddress, "[Cópia] " + subject, body);
        }
        return original;
    }
}
