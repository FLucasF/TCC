package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final SentMessages sentMessages;
    private final boolean testMode;

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

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (testMode) {
            actualTo = TEST_EMAIL;
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = sentMessages.add("EMAIL", actualTo, actualSubject, actualBody, "SENT");

        if (!testMode) {
            sentMessages.add("EMAIL", ARCHIVE_EMAIL, "[Cópia] " + subject, body, "SENT");
        }

        return sent;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        long atCount = email.chars().filter(ch -> ch == '@').count();
        if (atCount != 1) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex == 0) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
