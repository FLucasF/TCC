package hotel.messages;

/** A message to be processed before being sent. */
public record OutgoingMessage(String to, String subject, String body) {
}
