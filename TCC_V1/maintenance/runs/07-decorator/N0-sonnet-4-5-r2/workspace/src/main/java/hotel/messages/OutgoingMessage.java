package hotel.messages;

public record OutgoingMessage(String to, String subject, String body, MessageStatus status) {

    public static OutgoingMessage create(String to, String subject, String body) {
        return new OutgoingMessage(to, subject, body, MessageStatus.SENT);
    }

    public OutgoingMessage withStatus(MessageStatus newStatus) {
        return new OutgoingMessage(to, subject, body, newStatus);
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
}
