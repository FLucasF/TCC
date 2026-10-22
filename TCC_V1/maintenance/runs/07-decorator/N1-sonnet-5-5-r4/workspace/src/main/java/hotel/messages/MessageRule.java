package hotel.messages;

import java.util.List;

/**
 * One rule of the inn about outgoing messages. It answers with the drafts that take the place of the one it
 * received: the same draft changed, the draft blocked, or the draft followed by more drafts. Blocked drafts
 * never reach a rule.
 */
public interface MessageRule {

    List<Draft> apply(Draft draft);
}
