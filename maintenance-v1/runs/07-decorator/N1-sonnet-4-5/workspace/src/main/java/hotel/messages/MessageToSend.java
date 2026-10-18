package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * A message ready to be saved, along with any additional messages that should be saved right after it.
 */
final class MessageToSend {
    private final String to;
    private final String subject;
    private final String body;
    private final MessageStatus status;
    private final List<MessageToSend> additionalMessages;

    MessageToSend(String to, String subject, String body, MessageStatus status) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.status = status;
        this.additionalMessages = new ArrayList<>();
    }

    String to() {
        return to;
    }

    String subject() {
        return subject;
    }

    String body() {
        return body;
    }

    MessageStatus status() {
        return status;
    }

    List<MessageToSend> additionalMessages() {
        return additionalMessages;
    }

    void addAdditionalMessage(MessageToSend message) {
        additionalMessages.add(message);
    }

    MessageToSend withModified(String newTo, String newSubject, String newBody) {
        MessageToSend modified = new MessageToSend(newTo, newSubject, newBody, this.status);
        modified.additionalMessages.addAll(this.additionalMessages);
        return modified;
    }

    MessageToSend asBlocked() {
        return new MessageToSend(this.to, this.subject, this.body, MessageStatus.BLOCKED);
    }
}
