package hotel.messages;

import java.util.List;

/**
 * A rule about the e-mails the inn sends. A rule may block a message, rewrite it, or ask for extra copies of it;
 * it only takes part in the modes its scope allows. To add a rule, implement this and list it in {@link EmailSender}.
 */
public interface MessageRule {

    RuleScope scope();

    /** Judged on the message as originally requested; a blocked message is kept as is and not sent. */
    default boolean blocks(Outgoing message) {
        return false;
    }

    /** Changes a message that was not blocked. Rewrites run in order, each on the previous result. */
    default Outgoing rewrite(Outgoing message) {
        return message;
    }

    /** Extra messages to send right after the one that went out (judged on the message as sent). */
    default List<Outgoing> copies(Outgoing message) {
        return List.of();
    }
}
