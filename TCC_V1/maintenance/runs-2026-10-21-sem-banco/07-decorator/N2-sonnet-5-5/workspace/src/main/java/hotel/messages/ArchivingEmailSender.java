package hotel.messages;

/** Every message sent also generates a copy for the accountant's archive. Blocked messages have no copy. */
final class ArchivingEmailSender implements EmailSender {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private final EmailSender next;

    ArchivingEmailSender(EmailSender next) {
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
