package hotel.messages;

/** A message on its way out, before it is kept: the rules may block it, redirect it or add copies. */
public record OutgoingMessage(String to, String subject, String body, MessageStatus status) {

    public OutgoingMessage(String to, String subject, String body) {
        this(to, subject, body, MessageStatus.SENT);
    }

    public boolean blocked() {
        return status == MessageStatus.BLOCKED;
    }

    public OutgoingMessage blockedCopy() {
        return new OutgoingMessage(to, subject, body, MessageStatus.BLOCKED);
    }
}
