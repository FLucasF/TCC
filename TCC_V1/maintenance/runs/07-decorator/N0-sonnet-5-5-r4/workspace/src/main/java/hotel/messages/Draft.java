package hotel.messages;

/** An e-mail on its way through the sending rules. */
public record Draft(String to, String subject, String body) {
}
