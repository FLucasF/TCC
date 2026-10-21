package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * The data of an e-mail as it goes through the chain of handlers. Handlers can modify the message,
 * block it, or add copies that will also be stored.
 */
final class EmailContext {

    String to;
    String subject;
    String body;
    MessageStatus status = MessageStatus.SENT;
    private final List<MessageRecord> additionalMessages = new ArrayList<>();

    EmailContext(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    void addMessage(String to, String subject, String body, MessageStatus status) {
        additionalMessages.add(new MessageRecord(to, subject, body, status));
    }

    List<MessageRecord> additionalMessages() {
        return List.copyOf(additionalMessages);
    }

    record MessageRecord(String to, String subject, String body, MessageStatus status) {}
}
