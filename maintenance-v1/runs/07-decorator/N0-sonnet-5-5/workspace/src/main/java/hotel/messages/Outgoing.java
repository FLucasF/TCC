package hotel.messages;

/** An e-mail on its way out, before it is sent. */
public record Outgoing(String to, String subject, String body) {
}
