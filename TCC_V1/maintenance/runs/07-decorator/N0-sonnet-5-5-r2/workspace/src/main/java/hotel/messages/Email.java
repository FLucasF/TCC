package hotel.messages;

/** An e-mail before it is kept: who gets it, the subject and the text. */
public record Email(String to, String subject, String body) {
}
