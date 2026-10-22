package hotel.messages;

import java.util.List;

/**
 * One rule of the inn about the e-mails it sends. It gets a message that is still going out and answers with what
 * goes out in its place: the message changed, the message blocked, or the message plus others. Blocked messages
 * are never handed to a rule.
 */
public interface MessageRule {

    List<Outgoing> apply(Outgoing message);

    /** The rule only counts in the given mode; in the other one the message goes through untouched. */
    static MessageRule in(RuleMode mode, boolean testMode, MessageRule rule) {
        return mode.applies(testMode) ? rule : message -> List.of(message);
    }
}
