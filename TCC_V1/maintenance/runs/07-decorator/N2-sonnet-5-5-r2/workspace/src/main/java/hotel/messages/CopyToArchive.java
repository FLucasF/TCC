package hotel.messages;

/** Every message that was sent also goes, as a copy, to the accountant's archive. Blocked ones have no copy. */
public final class CopyToArchive implements MessageSender {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private final MessageSender next;

    public CopyToArchive(MessageSender next) {
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
