package hotel.messages;

public final class EmailToSend {
    private String to;
    private String subject;
    private String body;

    public EmailToSend(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
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
}
