package hotel.messages;

/** Does not send to a malformed address: the message is kept as blocked, with the address as it was typed. */
public final class AddressValidation implements MessageSender {

    private final MessageSender next;
    private final SentMessages sentMessages;

    public AddressValidation(MessageSender next, SentMessages sentMessages) {
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
    private static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && domain.lastIndexOf('.') < domain.length() - 1 && !domain.endsWith(".");
    }
}
