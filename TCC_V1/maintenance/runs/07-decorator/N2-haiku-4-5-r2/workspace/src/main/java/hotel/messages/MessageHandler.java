package hotel.messages;

import java.util.List;

/** A handler in the message processing chain. Each handler can validate, transform, or block a message. */
public interface MessageHandler {
    /**
     * Process a message.
     *
     * @return a list of messages to store (empty = blocked, 1 = passed, 2+ = original + copies)
     */
    List<MessageToStore> handle(MessageToStore message);
}
