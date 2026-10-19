package pousada.messaging;

public record Message(int id, Channel channel, String to, String subject, String body, MessageStatus status) {

    public static Message email(String to, String subject, String body) {
        return new Message(0, Channel.EMAIL, to, subject, body, MessageStatus.SENT);
    }

    public Message withId(int newId) {
        return new Message(newId, channel, to, subject, body, status);
    }

    public Message withStatus(MessageStatus newStatus) {
        return new Message(id, channel, to, subject, body, newStatus);
    }

    public Message withTo(String newTo) {
        return new Message(id, channel, newTo, subject, body, status);
    }

    public Message withSubject(String newSubject) {
        return new Message(id, channel, to, newSubject, body, status);
    }

    public Message withBody(String newBody) {
        return new Message(id, channel, to, subject, newBody, status);
    }
}
