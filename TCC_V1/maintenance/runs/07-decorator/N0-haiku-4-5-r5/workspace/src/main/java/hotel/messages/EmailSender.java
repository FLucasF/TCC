package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, "true".equals(System.getenv("HOTEL_TEST_MODE")));
    }

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.testMode = testMode;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, "BLOCKED");
        }

        Message sentMessage;
        if (testMode) {
            String testBody = "Destinatário original: " + to + "\n" + body;
            sentMessage = sentMessages.add("EMAIL", "testes@pousadamarealta.com.br", "[TESTE] " + subject, testBody);
        } else {
            sentMessage = sentMessages.add("EMAIL", to, subject, body);
            sentMessages.add("EMAIL", "arquivo@pousadamarealta.com.br", "[Cópia] " + subject, body);
        }

        return sentMessage;
    }
}
