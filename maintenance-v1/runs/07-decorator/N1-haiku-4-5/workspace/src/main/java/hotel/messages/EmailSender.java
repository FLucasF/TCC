package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }

        String testMode = System.getenv("HOTEL_TEST_MODE");
        boolean isTestMode = "true".equals(testMode);

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (isTestMode) {
            finalTo = "testes@pousadamarealta.com.br";
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
        }

        Message sent = sentMessages.add("EMAIL", finalTo, finalSubject, finalBody, MessageStatus.SENT);

        if (!isTestMode) {
            sentMessages.add("EMAIL", "arquivo@pousadamarealta.com.br", "[Cópia] " + subject, body, MessageStatus.SENT);
        }

        return sent;
    }
}
