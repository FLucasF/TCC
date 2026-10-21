package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** A message being prepared to send, with any additional copies it should generate. */
final class OutboundMessage {
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<OutboundMessage> copies = new ArrayList<>();

    OutboundMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
    }

    String to() {
        return to;
    }

    void setTo(String to) {
        this.to = to;
    }

    String subject() {
        return subject;
    }

    void setSubject(String subject) {
        this.subject = subject;
    }

    String body() {
        return body;
    }

    void setBody(String body) {
        this.body = body;
    }

    MessageStatus status() {
        return status;
    }

    void block() {
        this.status = MessageStatus.BLOCKED;
    }

    void addCopy(OutboundMessage copy) {
        copies.add(copy);
    }

    List<OutboundMessage> copies() {
        return new ArrayList<>(copies);
    }
}
