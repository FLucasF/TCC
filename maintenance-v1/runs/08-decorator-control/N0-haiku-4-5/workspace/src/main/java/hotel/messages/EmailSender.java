package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String SIGNATURE = """

            --
            Pousada Maré Alta
            (79) 3200-0000 | @pousadamarealta""";

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body + SIGNATURE);
    }
}
