package hotel.messages;

/**
 * A rule that can block, modify, or add messages during the sending process.
 * Rules can apply always, only in test mode, or only in production mode.
 */
interface MessageRule {

    /**
     * Applies the rule to the message. Can block it, modify it, or add additional messages.
     * Rules are applied in sequence, each receiving the result of the previous one.
     */
    MessageToSend apply(MessageToSend message, boolean testMode);
}
