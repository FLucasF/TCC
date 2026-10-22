package hotel.messages;

/** Does not send to a mistyped address: keeps the message as blocked, so the front desk can call the guest. */
public final class ValidAddressOnly implements MessageSender {

    private final MessageSender next;
    private final SentMessages sentMessages;

    public ValidAddressOnly(MessageSender next, SentMessages sentMessages) {
        this.next = next;
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        if (isValid(to)) {
            return next.send(to, subject, body);
        }
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
    }

    /** One single @, something before it, and after it a name with a dot in the middle. */
    private static boolean isValid(String address) {
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.indexOf('.', 1) > 0 && !domain.startsWith(".") && !domain.endsWith(".");
    }
}
