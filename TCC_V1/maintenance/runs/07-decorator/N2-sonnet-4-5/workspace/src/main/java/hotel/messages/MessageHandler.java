package hotel.messages;

/** A handler in the message processing chain. Each handler can validate, transform or generate additional messages. */
public interface MessageHandler {

    /**
     * Process a message. The handler can:
     * - Block it (return ProcessedMessage.blocked)
     * - Transform it (return ProcessedMessage.sent with modified content)
     * - Add additional messages (return ProcessedMessage.multiple with original + copies)
     * - Pass it unchanged (return ProcessedMessage.sent with same content)
     */
    ProcessedMessage handle(OutgoingMessage message);
}
