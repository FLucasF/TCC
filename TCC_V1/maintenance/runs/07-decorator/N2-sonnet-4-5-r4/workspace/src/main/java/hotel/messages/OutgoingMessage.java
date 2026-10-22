package hotel.messages;

/**
 * A message being prepared for sending. Rules can modify its fields or mark it as blocked.
 */
public final class OutgoingMessage {
    private String to;
    private String subject;
    private String body;
    private boolean blocked;

    public OutgoingMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.blocked = false;
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

    public boolean isBlocked() {
        return blocked;
    }

    public void changeTo(String newTo) {
        this.to = newTo;
    }

    public void changeSubject(String newSubject) {
        this.subject = newSubject;
    }

    public void changeBody(String newBody) {
        this.body = newBody;
    }

    public void block() {
        this.blocked = true;
    }
}
