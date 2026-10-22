package hotel.messages;

/** Does not send to a mistyped address: the message is kept as blocked, for the front desk to call the guest. */
public final class ValidatingEmailSender implements EmailSender {

    private final EmailSender next;
    private final SentMessages sentMessages;

    public ValidatingEmailSender(EmailSender next, SentMessages sentMessages) {
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

    /** One @, something before it, and after it a name with a dot in the middle. */
    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && domain.charAt(domain.length() - 1) != '.';
    }
}
