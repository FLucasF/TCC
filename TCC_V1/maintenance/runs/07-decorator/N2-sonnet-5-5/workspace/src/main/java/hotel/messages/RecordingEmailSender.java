package hotel.messages;

/** The e-mail itself: sends to the address it is given and keeps the message as sent. */
public final class RecordingEmailSender implements EmailSender {

    private final SentMessages sentMessages;

    public RecordingEmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);
    }
}
