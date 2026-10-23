package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Uma mensagem pronta para ser enviada, antes de ser gravada. */
public final class OutgoingMessage {

    private final String channel;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<OutgoingMessage> additionalMessages;

    public OutgoingMessage(String channel, String to, String subject, String body) {
        this.channel = channel;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
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

    public MessageStatus status() {
        return status;
    }

    public List<OutgoingMessage> additionalMessages() {
        return additionalMessages;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void block() {
        this.status = MessageStatus.BLOCKED;
    }

    public void addMessage(OutgoingMessage message) {
        this.additionalMessages.add(message);
    }
}
