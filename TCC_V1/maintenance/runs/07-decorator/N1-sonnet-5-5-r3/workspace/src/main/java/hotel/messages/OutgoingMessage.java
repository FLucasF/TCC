package hotel.messages;

/** A message on its way out, before it is kept: the rules may change it, block it or add copies of it. */
public record OutgoingMessage(String to, String subject, String body, MessageStatus status) {

    public OutgoingMessage(String to, String subject, String body) {
        this(to, subject, body, MessageStatus.SENT);
    }

    public OutgoingMessage withTo(String newTo) {
        return new OutgoingMessage(newTo, subject, body, status);
    }

    public OutgoingMessage withSubject(String newSubject) {
        return new OutgoingMessage(to, newSubject, body, status);
    }

    public OutgoingMessage withBody(String newBody) {
        return new OutgoingMessage(to, subject, newBody, status);
    }

    public OutgoingMessage blocked() {
        return new OutgoingMessage(to, subject, body, MessageStatus.BLOCKED);
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }
}
