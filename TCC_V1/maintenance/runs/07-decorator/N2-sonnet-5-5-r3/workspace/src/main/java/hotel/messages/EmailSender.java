package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender implements MessageSender {

    private final SentMessages sentMessages;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);
    }
}
