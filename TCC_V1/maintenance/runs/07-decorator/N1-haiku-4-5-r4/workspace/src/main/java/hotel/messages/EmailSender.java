package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final SentMessages sentMessages;
    private final boolean isTestMode;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE")));
    }

    public EmailSender(SentMessages sentMessages, boolean isTestMode) {
        this.sentMessages = sentMessages;
        this.isTestMode = isTestMode;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, "BLOCKED");
        }

        Message result;

        if (isTestMode) {
            result = sentMessages.add("EMAIL", TEST_EMAIL, "[TESTE] " + subject, "Destinatário original: " + to + "\n" + body, "SENT");
        } else {
            result = sentMessages.add("EMAIL", to, subject, body, "SENT");
            sentMessages.add("EMAIL", ARCHIVE_EMAIL, "[Cópia] " + subject, body, "SENT");
        }

        return result;
    }
}
