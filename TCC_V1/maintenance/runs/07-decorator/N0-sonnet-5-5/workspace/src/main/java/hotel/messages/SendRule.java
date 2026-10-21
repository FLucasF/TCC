package hotel.messages;

import java.util.List;
import java.util.Optional;

/** A rule applied to every e-mail. All methods are optional; the defaults do nothing. */
public interface SendRule {

    /** A reason to block the message (it is then recorded as BLOCKED, with the original address). */
    default Optional<String> blockReason(Outgoing message) {
        return Optional.empty();
    }

    /** Changes the message before it goes out (only for messages not blocked). */
    default Outgoing rewrite(Outgoing message) {
        return message;
    }

    /** Extra messages to send right after the given one went out. */
    default List<Outgoing> copiesOf(Outgoing sent) {
        return List.of();
    }

    /** Called after the message was really sent. */
    default void sent(Outgoing message) {
    }
}
