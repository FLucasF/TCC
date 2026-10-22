package hotel.messages;

import java.util.List;

/**
 * One rule of the inn's e-mail policy. It receives a message and returns what goes out instead: the message
 * changed, blocked, or followed by extra messages. Blocked messages are left alone by the rules that come after.
 */
public interface EmailRule {

    List<OutgoingMessage> apply(OutgoingMessage message);
}
