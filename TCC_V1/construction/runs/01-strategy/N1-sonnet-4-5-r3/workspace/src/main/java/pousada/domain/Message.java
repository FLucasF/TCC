package pousada.domain;

public class Message {
    private Long id;
    private final String channel;
    private final String to;
    private final String subject;
    private final String body;

    public Message(Long id, String channel, String to, String subject, String body) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long id() {
        return id;
    }

    public String channel() {
        return channel;
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
}
