package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;
    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE")));
    }

    EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.testMode = testMode;
    }

    public Message send(String to, String subject, String body) {
        if (!isValidEmail(to)) {
            return sentMessages.add("EMAIL", to, subject, body, "BLOCKED");
        }

        if (testMode) {
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + to + "\n\n" + body;
            return sentMessages.add("EMAIL", TEST_ADDRESS, testSubject, testBody, "SENT");
        }

        Message sent = sentMessages.add("EMAIL", to, subject, body, "SENT");
        sentMessages.add("EMAIL", ARCHIVE_ADDRESS, "[Cópia] " + subject, body, "SENT");
        return sent;
    }

    private static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex < 1 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
