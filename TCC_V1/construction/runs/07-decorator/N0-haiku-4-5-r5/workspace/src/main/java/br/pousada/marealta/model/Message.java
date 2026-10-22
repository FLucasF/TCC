package br.pousada.marealta.model;

public class Message {
    private long id;
    private MessageChannel channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;

    public Message(long id, MessageChannel channel, String to, String subject, String body, MessageStatus status) {
        this.id = id;
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
    }

    public long getId() { return id; }
    public MessageChannel getChannel() { return channel; }
    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public MessageStatus getStatus() { return status; }
}
