package hotel.messages;

/**
 * An email being prepared for sending. Rules can modify or block it before it actually goes out.
 */
public final class OutgoingEmail {
    private final String originalTo;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public OutgoingEmail(String to, String subject, String body) {
        this.originalTo = to;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
    }

    public String originalTo() {
        return originalTo;
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

    public void block() {
        this.status = MessageStatus.BLOCKED;
    }
}
