package hotel.messages;

/** Every message sent also goes, as a copy, to the address the accountant keeps. Blocked messages have no copy. */
public final class ArchiveCopy implements MessageSender {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private final MessageSender next;

    public ArchiveCopy(MessageSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message message = next.send(to, subject, body);
        if (message.status() == MessageStatus.SENT) {
            next.send(ARCHIVE_ADDRESS, "[Cópia] " + subject, body);
        }
        return message;
    }
}
