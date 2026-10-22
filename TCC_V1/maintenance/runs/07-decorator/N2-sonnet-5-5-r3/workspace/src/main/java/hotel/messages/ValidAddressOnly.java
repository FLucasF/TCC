package hotel.messages;

/** Does not send to an invalid address: the message is kept as blocked, so the front desk can call the guest. */
final class ValidAddressOnly implements MessageSender {

    private final MessageSender next;
    private final SentMessages sentMessages;

    ValidAddressOnly(MessageSender next, SentMessages sentMessages) {
        this.next = next;
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        if (!isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }
        return next.send(to, subject, body);
    }

    /** A single @, something before it and, after it, a name with a dot in the middle. */
    private static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        return domain.indexOf('.') > 0 && lastDot < domain.length() - 1;
    }
}
