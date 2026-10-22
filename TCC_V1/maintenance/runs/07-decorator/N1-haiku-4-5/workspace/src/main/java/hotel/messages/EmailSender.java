package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final SentMessages sentMessages;
    private final HotelEnvironment env;

    public EmailSender(SentMessages sentMessages, HotelEnvironment env) {
        this.sentMessages = sentMessages;
        this.env = env;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED.name());
        }

        Message sent;
        if (env.isTestMode()) {
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + to + "\n" + body;
            sent = sentMessages.add("EMAIL", TEST_EMAIL, testSubject, testBody, MessageStatus.SENT.name());
        } else {
            sent = sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT.name());
            sentMessages.add("EMAIL", ARCHIVE_EMAIL, "[Cópia] " + subject, body, MessageStatus.SENT.name());
        }
        return sent;
    }
}
