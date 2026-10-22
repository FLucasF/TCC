package hotel.messages;

public final class OutgoingMessage {
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public OutgoingMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
    }

    public String to() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String subject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String body() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MessageStatus status() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}
