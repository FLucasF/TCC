package hotel.messages;

/** An e-mail on its way out, which the rules may block, change or accompany with other e-mails. */
public record Draft(String to, String subject, String body, MessageStatus status) {

    public static Draft of(String to, String subject, String body) {
        return new Draft(to, subject, body, MessageStatus.SENT);
    }

    public boolean blocked() {
        return status == MessageStatus.BLOCKED;
    }

    public Draft asBlocked() {
        return new Draft(to, subject, body, MessageStatus.BLOCKED);
    }

    public Draft withTo(String newTo) {
        return new Draft(newTo, subject, body, status);
    }

    public Draft withSubject(String newSubject) {
        return new Draft(to, newSubject, body, status);
    }

    public Draft withBody(String newBody) {
        return new Draft(to, subject, newBody, status);
    }
}
