package hotel.messages;

/** A message that may be stored: includes the recipient, subject, body, and status. */
public record MessageToStore(String to, String subject, String body, String status) {
}
