package hotel.messages;

/** Sends e-mails to guests. Every e-mail ends with the inn's signature and is kept in the list of sent messages. */
public final class EmailSender {

    /** Closes every message. Change it here (e.g. for Christmas). */
    static final String SIGNATURE = "--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta";

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body.stripTrailing() + "\n\n" + SIGNATURE);
    }
}
