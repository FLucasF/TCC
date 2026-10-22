package hotel.messages;

/** Every message sent also goes, as a copy, to the accountant's archive. A blocked message has no copy. */
public final class ArchivingEmailSender implements EmailSender {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private final EmailSender next;

    public ArchivingEmailSender(EmailSender next) {
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        Message original = next.send(to, subject, body);
        if (original.status() == MessageStatus.SENT) {
            next.send(ARCHIVE_ADDRESS, "[Cópia] " + subject, body);
        }
        return original;
    }
}
