package hotel.messages;

/** A message to send, before the filters decide what happens to it. */
public record OutgoingMessage(String to, String subject, String body) {
}
