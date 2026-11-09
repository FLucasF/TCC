package hotel.messages;

/** Sends e-mails to guests, each ending with the inn's signature. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String SIGNATURE = "--\nPousada Maré Alta\n(79) 3200-0000 | @pousadamarealta\n";

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    public Message send(String to, String subject, String body) {
        String text = body.endsWith("\n") ? body : body + "\n";
        return sentMessages.add("EMAIL", to, subject, text + "\n" + SIGNATURE);
    }
}
