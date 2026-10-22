package hotel.messages;

import java.util.List;

/**
 * A rule that processes an outgoing message. It can modify the message, block it, or create additional
 * messages (like copies for archiving).
 */
public interface MessageRule {
    /**
     * Process the message. Returns the original message (possibly modified or blocked) plus any additional
     * messages that should be sent (like archive copies).
     */
    List<OutgoingMessage> process(OutgoingMessage message);
}
