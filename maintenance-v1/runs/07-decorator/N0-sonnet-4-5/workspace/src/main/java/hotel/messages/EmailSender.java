package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final SentMessages sentMessages;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
    }

    public Message send(String to, String subject, String body) {
        // Block invalid e-mail addresses
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }

        // In test mode, redirect to test address
        if (testMode) {
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + to + "\n" + body;
            return sentMessages.add("EMAIL", TEST_EMAIL, testSubject, testBody, MessageStatus.SENT);
        }

        // Normal mode: send to real address
        Message original = sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);

        // Create archive copy
        String archiveSubject = "[Cópia] " + subject;
        sentMessages.add("EMAIL", ARCHIVE_EMAIL, archiveSubject, body, MessageStatus.SENT);

        return original;
    }
}
