package hotel.messages;

/** A message to be sent, before it's saved. */
public final class OutgoingMessage {

    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;

    public OutgoingMessage(String to, String subject, String body) {
        this(to, subject, body, MessageStatus.SENT);
    }

    public OutgoingMessage(String to, String subject, String body, MessageStatus status) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public String to() {
        return to;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public MessageStatus status() {
        return status;
    }

    public OutgoingMessage withRecipient(String newRecipient) {
        return new OutgoingMessage(newRecipient, subject, body, status);
    }

    public OutgoingMessage withSubject(String newSubject) {
        return new OutgoingMessage(to, newSubject, body, status);
    }

    public OutgoingMessage withBody(String newBody) {
        return new OutgoingMessage(to, subject, newBody, status);
    }

    public OutgoingMessage withStatus(MessageStatus newStatus) {
        return new OutgoingMessage(to, subject, body, newStatus);
    }
}
