package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final MessageConfig config;

    public EmailSender(SentMessages sentMessages, MessageConfig config) {
        this.sentMessages = sentMessages;
        this.config = config;
    }

    public Message send(String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED.name());
        }

        String actualRecipient = to;
        String actualSubject = subject;
        String actualBody = body;

        if (config.isTestMode()) {
            actualRecipient = config.getTestEmail();
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        Message message = sentMessages.add("EMAIL", actualRecipient, actualSubject, actualBody, MessageStatus.SENT.name());

        if (!config.isTestMode() && config.isArchiveEnabled()) {
            sentMessages.add("EMAIL", config.getArchiveEmail(), "[Cópia] " + subject, body, MessageStatus.SENT.name());
        }

        return message;
    }
}
