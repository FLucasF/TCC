package hotel.messages;

final class OutgoingMessage {
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    OutgoingMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
    }

    String to() {
        return to;
    }

    void changeTo(String newTo) {
        this.to = newTo;
    }

    String subject() {
        return subject;
    }

    void prependSubject(String prefix) {
        this.subject = prefix + subject;
    }

    String body() {
        return body;
    }

    void prependBody(String prefix) {
        this.body = prefix + body;
    }

    MessageStatus status() {
        return status;
    }

    void block() {
        this.status = MessageStatus.BLOCKED;
    }
}
