package hotel.messages;

import java.util.List;

/**
 * A rule of the inn about the e-mails it sends. A rule may change or block the message before it is sent, and may
 * ask for extra messages once it was sent. A new rule is a new class, listed in {@link MessageRules}.
 */
public interface MessageRule {

    default Draft apply(Draft draft) {
        return draft;
    }

    /** Messages to send in addition to the given one, which was sent. */
    default List<Draft> extrasFor(Draft sent) {
        return List.of();
    }
}
