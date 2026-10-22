package hotel.messages;

/** A message on its way out, before the rules have had their say about it. */
public record Draft(String to, String subject, String body, MessageStatus status) {

    public static Draft email(String to, String subject, String body) {
        return new Draft(to, subject, body, MessageStatus.SENT);
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }

    public Draft blocked() {
        return new Draft(to, subject, body, MessageStatus.BLOCKED);
    }
}
