package hotel.messages;

/** A message on its way out, which the rules may change or block before it is kept. */
public record Draft(String to, String subject, String body, MessageStatus status) {

    public Draft(String to, String subject, String body) {
        this(to, subject, body, MessageStatus.SENT);
    }

    public Draft blocked() {
        return new Draft(to, subject, body, MessageStatus.BLOCKED);
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }
}
