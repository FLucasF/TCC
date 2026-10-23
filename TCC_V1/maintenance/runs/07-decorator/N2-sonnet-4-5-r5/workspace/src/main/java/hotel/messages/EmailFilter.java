package hotel.messages;

/**
 * A rule that decides what happens to an outgoing message: it can go through (unchanged or transformed),
 * be blocked, or generate extra messages.
 */
public interface EmailFilter {
    FilterResult apply(OutgoingMessage message);
}
