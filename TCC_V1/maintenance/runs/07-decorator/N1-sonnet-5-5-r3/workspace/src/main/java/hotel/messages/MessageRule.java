package hotel.messages;

import java.util.List;

/**
 * One rule of the inn about its e-mails. It gets a message that is still going out and answers with the messages
 * that go out in its place: the same one changed, blocked, or followed by copies.
 */
public interface MessageRule {

    List<OutgoingMessage> apply(OutgoingMessage message);
}
