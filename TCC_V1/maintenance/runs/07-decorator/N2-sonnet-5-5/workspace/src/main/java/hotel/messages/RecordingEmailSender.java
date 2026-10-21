package hotel.messages;

/** The real sending: the e-mail goes out and is kept in the list of sent messages. */
final class RecordingEmailSender implements EmailSender {

    private final SentMessages sentMessages;

    RecordingEmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);
    }
}
