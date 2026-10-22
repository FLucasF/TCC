package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** The state of a message as it flows through the sending rules. */
final class MessageContext {

    private OutgoingMessage message;
    private MessageStatus status;
    private final List<OutgoingMessage> additionalMessages;

    MessageContext(OutgoingMessage message) {
        this.message = message;
        this.status = MessageStatus.SENT;
        this.additionalMessages = new ArrayList<>();
    }

    OutgoingMessage message() {
        return message;
    }

    MessageStatus status() {
        return status;
    }

    List<OutgoingMessage> additionalMessages() {
        return additionalMessages;
    }

    void block() {
        this.status = MessageStatus.BLOCKED;
    }

    boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }

    void transformMessage(OutgoingMessage transformed) {
        this.message = transformed;
    }

    void addAdditionalMessage(OutgoingMessage message) {
        this.additionalMessages.add(message);
    }
}
