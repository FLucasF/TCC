package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, false);
    }

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.testMode = testMode;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (testMode) {
            actualTo = "testes@pousadamarealta.com.br";
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message message = sentMessages.add("EMAIL", actualTo, actualSubject, actualBody, MessageStatus.SENT);

        if (!testMode) {
            sentMessages.add("EMAIL", "arquivo@pousadamarealta.com.br", "[Cópia] " + subject, body, MessageStatus.SENT);
        }

        return message;
    }
}
