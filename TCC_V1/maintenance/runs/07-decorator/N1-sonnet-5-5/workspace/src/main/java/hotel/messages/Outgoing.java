package hotel.messages;

/** An e-mail on its way out, before it is kept: the message rules may change it, block it or add others. */
public record Outgoing(String to, String subject, String body, MessageStatus status) {

    public static Outgoing toSend(String to, String subject, String body) {
        return new Outgoing(to, subject, body, MessageStatus.SENT);
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }

    public Outgoing blocked() {
        return new Outgoing(to, subject, body, MessageStatus.BLOCKED);
    }
}
