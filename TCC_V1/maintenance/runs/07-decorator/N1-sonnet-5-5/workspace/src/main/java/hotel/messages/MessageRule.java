package hotel.messages;

import java.util.List;

/**
 * One rule applied to an e-mail before it goes out. It returns what must be sent in its place: the same
 * draft changed, the draft blocked, or the draft followed by extra drafts. Rules never see blocked drafts.
 */
public interface MessageRule {

    List<Draft> apply(Draft draft);
}
