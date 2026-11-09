package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    /** Closes every message the inn sends. Change it here when the signature changes. */
    static final String SIGNATURE = "--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n";

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, withSignature(body));
    }

    private static String withSignature(String body) {
        return body.stripTrailing() + "\n\n" + SIGNATURE;
    }
}
