package hotel.messages;

/**
 * One step in the chain that processes an e-mail before it is sent. A handler can modify the message,
 * block it (mark it as BLOCKED), or add extra messages (such as copies).
 */
interface EmailHandler {

    /**
     * Processes the e-mail. Returns true if the next handler should run, or false to stop the chain
     * (for instance, when the message is blocked).
     */
    boolean handle(EmailContext context);
}
