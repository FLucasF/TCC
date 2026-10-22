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

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (testMode) {
            finalTo = TEST_EMAIL;
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = sentMessages.add("EMAIL", finalTo, finalSubject, finalBody, "SENT");

        if (!testMode) {
            sentMessages.add("EMAIL", ARCHIVE_EMAIL, "[Cópia] " + subject, body, "SENT");
        }

        return sent;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        if (localPart.isEmpty() || domainPart.isEmpty()) {
            return false;
        }
        return domainPart.contains(".");
    }
}
