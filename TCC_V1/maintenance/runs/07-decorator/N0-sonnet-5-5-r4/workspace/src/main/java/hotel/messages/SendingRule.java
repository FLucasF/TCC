package hotel.messages;

import java.util.List;
import java.util.Optional;

/**
 * One rule about the e-mails the inn sends. A rule can do any of three things, and each has a neutral default:
 * block the message, rewrite it before it goes out, or ask for extra messages once it was sent.
 */
public interface SendingRule {

    /** The reason to block the message (it is then kept, not sent), judged on the message as originally written. */
    default Optional<String> blocks(Draft original) {
        return Optional.empty();
    }

    /** The message that really goes out. */
    default Draft rewrite(Draft draft) {
        return draft;
    }

    /** Extra messages to send because this one was sent. */
    default List<Draft> copiesOf(Draft sent) {
        return List.of();
    }
}
