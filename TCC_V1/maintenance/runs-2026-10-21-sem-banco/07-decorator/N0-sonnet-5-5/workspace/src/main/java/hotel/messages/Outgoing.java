package hotel.messages;

/** An e-mail on its way out. {@code originalTo} is the address the caller asked for; {@code to} may be redirected. */
public record Outgoing(String originalTo, String to, String subject, String body) {

    public static Outgoing of(String to, String subject, String body) {
        return new Outgoing(to, to, subject, body);
    }

    public Outgoing with(String newTo, String newSubject, String newBody) {
        return new Outgoing(originalTo, newTo, newSubject, newBody);
    }
}
