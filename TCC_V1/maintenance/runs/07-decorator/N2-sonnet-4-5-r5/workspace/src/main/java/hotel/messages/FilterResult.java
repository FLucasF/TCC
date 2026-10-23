package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * What happened to a message after the filters processed it: it can go through (possibly transformed),
 * be blocked, or generate extra messages.
 */
public final class FilterResult {

    private final OutgoingMessage message;
    private final MessageStatus status;
    private final List<OutgoingMessage> extraMessages;

    private FilterResult(OutgoingMessage message, MessageStatus status, List<OutgoingMessage> extraMessages) {
        this.message = message;
        this.status = status;
        this.extraMessages = List.copyOf(extraMessages);
    }

    /** The message goes through, unchanged or transformed. */
    public static FilterResult allow(OutgoingMessage message) {
        return new FilterResult(message, MessageStatus.SENT, List.of());
    }

    /** The message goes through and generates extra messages (like a copy for archiving). */
    public static FilterResult allowWithExtra(OutgoingMessage message, OutgoingMessage... extra) {
        return new FilterResult(message, MessageStatus.SENT, List.of(extra));
    }

    /** The message is blocked. */
    public static FilterResult block(OutgoingMessage message) {
        return new FilterResult(message, MessageStatus.BLOCKED, List.of());
    }

    public OutgoingMessage message() {
        return message;
    }

    public MessageStatus status() {
        return status;
    }

    public List<OutgoingMessage> extraMessages() {
        return extraMessages;
    }

    public boolean isBlocked() {
        return status == MessageStatus.BLOCKED;
    }
}
