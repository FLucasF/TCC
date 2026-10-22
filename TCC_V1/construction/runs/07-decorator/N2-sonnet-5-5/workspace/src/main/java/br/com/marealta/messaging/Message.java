package br.com.marealta.messaging;

public record Message(Long id, Channel channel, String to, String subject, String body, Status status) {
    public enum Channel { EMAIL }

    public enum Status { SENT, BLOCKED }

    public static Message email(String to, String subject, String body) {
        return new Message(null, Channel.EMAIL, to, subject, body, Status.SENT);
    }

    public Message withTo(String to) { return new Message(id, channel, to, subject, body, status); }
    public Message withSubject(String subject) { return new Message(id, channel, to, subject, body, status); }
    public Message withBody(String body) { return new Message(id, channel, to, subject, body, status); }
    public Message withStatus(Status status) { return new Message(id, channel, to, subject, body, status); }
}
