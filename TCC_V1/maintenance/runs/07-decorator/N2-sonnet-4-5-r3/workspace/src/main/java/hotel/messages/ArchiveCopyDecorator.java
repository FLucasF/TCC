package hotel.messages;

/** Creates an archive copy of every sent message. Blocked messages are not archived. */
final class ArchiveCopyDecorator implements MessageSender {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    private final MessageSender next;

    ArchiveCopyDecorator(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message original = next.send(to, subject, body);
        if (original.status() == MessageStatus.SENT) {
            next.send(ARCHIVE_EMAIL, "[Cópia] " + subject, body);
        }
        return original;
    }
}
