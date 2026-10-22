package hotel.messages;

/** Sends messages by saving them to the message store. */
final class BasicMessageSender implements MessageSender {

    private final SentMessages sentMessages;

    BasicMessageSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
    }

    @Override
    public Message send(String to, String subject, String body) {
        return sentMessages.add("EMAIL", to, subject, body, MessageStatus.SENT);
    }
}
