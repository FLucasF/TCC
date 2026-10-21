package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final SentMessages sentMessages;
    private final TestModeProvider testModeProvider;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, TestModeProvider.create());
    }

    public EmailSender(SentMessages sentMessages, TestModeProvider testModeProvider) {
        this.sentMessages = sentMessages;
        this.testModeProvider = testModeProvider;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, "BLOCKED");
        }

        if (testModeProvider.isTestMode()) {
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + to + "\n" + body;
            return sentMessages.add("EMAIL", TEST_EMAIL, testSubject, testBody, "SENT");
        }

        Message sent = sentMessages.add("EMAIL", to, subject, body, "SENT");
        String archiveSubject = "[Cópia] " + subject;
        sentMessages.add("EMAIL", ARCHIVE_EMAIL, archiveSubject, body, "SENT");
        return sent;
    }
}
