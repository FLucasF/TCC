package hotel.messages;

/** Blocks messages to invalid addresses, so the front desk can phone the guest. */
final class ValidatingEmailSender implements EmailSender {

    private final EmailSender next;
    private final SentMessages sentMessages;

    ValidatingEmailSender(EmailSender next, SentMessages sentMessages) {
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

    /** One '@', something before it, and after it a name with a dot in the middle. */
    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || address.indexOf('@', at + 1) >= 0) {
            return false;
        }
        String domain = address.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        return domain.indexOf('.') > 0 && lastDot < domain.length() - 1 && !domain.startsWith(".");
    }
}
