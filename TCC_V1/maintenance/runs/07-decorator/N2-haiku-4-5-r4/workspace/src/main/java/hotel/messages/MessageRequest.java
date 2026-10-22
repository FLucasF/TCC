package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public class MessageRequest {
    private String originalTo;
    private String to;
    private String subject;
    private String body;
    private MessageStatus status;
    private final List<Message> generatedMessages;

    public MessageRequest(String to, String subject, String body) {
        this.originalTo = to;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = MessageStatus.SENT;
        this.generatedMessages = new ArrayList<>();
    }

    public String originalTo() {
        return originalTo;
    }

    public String to() {
        return to;
    }

    public void changeTo(String newTo) {
        this.to = newTo;
    }

    public String subject() {
        return subject;
    }

    public void changeSubject(String newSubject) {
        this.subject = newSubject;
    }

    public String body() {
        return body;
    }

    public void changeBody(String newBody) {
        this.body = newBody;
    }

    public MessageStatus status() {
        return status;
    }

    public void block() {
        this.status = MessageStatus.BLOCKED;
    }

    public List<Message> generatedMessages() {
        return generatedMessages;
    }

    public void addGeneratedMessage(Message message) {
        generatedMessages.add(message);
    }
}
