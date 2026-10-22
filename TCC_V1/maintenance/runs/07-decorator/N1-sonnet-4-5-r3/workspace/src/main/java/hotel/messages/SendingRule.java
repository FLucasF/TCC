package hotel.messages;

import java.util.List;

/**
 * A rule that processes outgoing messages. It can block a message, change it, or create additional
 * messages (like archive copies).
 */
public interface SendingRule {

    /**
     * Processes an outgoing message. Returns a list of messages to save: the original (possibly
     * modified or blocked) plus any additional messages this rule creates.
     */
    List<OutgoingMessage> apply(OutgoingMessage message);
}
