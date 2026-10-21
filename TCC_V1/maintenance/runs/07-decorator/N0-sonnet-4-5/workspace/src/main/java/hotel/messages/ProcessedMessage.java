package hotel.messages;

import java.util.ArrayList;
import java.util.List;

final class ProcessedMessage {
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<ProcessedMessage> derived;

    ProcessedMessage(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.derived = new ArrayList<>();
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

    void setStatus(MessageStatus status) {
        this.status = status;
    }

    List<ProcessedMessage> derived() {
        return new ArrayList<>(derived);
    }

    void addDerived(ProcessedMessage message) {
        derived.add(message);
    }
}
