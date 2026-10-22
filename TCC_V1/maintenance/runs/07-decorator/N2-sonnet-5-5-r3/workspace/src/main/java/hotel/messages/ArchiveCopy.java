package hotel.messages;

/** Every message sent also goes, as a copy, to the archive address. A blocked message has no copy. */
final class ArchiveCopy implements MessageSender {

    private final MessageSender next;
    private final String archiveAddress;

    ArchiveCopy(MessageSender next, String archiveAddress) {
        this.next = next;
        this.archiveAddress = archiveAddress;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message sent = next.send(to, subject, body);
        if (sent.status() == MessageStatus.SENT) {
            next.send(archiveAddress, "[Cópia] " + subject, body);
        }
        return sent;
    }
}
