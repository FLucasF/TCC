package hotel.messages;

/** Does not send to a mistyped address: keeps the message as BLOCKED, so the front desk calls the guest. */
public final class BlockInvalidAddress implements MessageSender {

    private final SentMessages sentMessages;
    private final MessageSender next;

    public BlockInvalidAddress(SentMessages sentMessages, MessageSender next) {
        this.sentMessages = sentMessages;
        this.next = next;
    }

    @Override
    public Message send(String to, String subject, String body) {
        if (isValid(to)) {
            return next.send(to, subject, body);
        }
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
    }

    /** One @, something before it, and after it a name with a dot in the middle (ana@gmail.com). */
    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        return domain.indexOf('.') > 0 && lastDot < domain.length() - 1;
    }
}
