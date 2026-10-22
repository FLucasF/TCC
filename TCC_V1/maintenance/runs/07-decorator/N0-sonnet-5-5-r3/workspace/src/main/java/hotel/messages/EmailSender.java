package hotel.messages;

/** Sends e-mails to guests, following the inn's e-mail policy. Every message is kept in the list of messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final EmailPolicy policy;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, EmailPolicy.standard(false));
    }

    public EmailSender(SentMessages sentMessages, EmailPolicy policy) {
        this.sentMessages = sentMessages;
        this.policy = policy;
    }

    /** Returns the message for the original recipient (sent or blocked); copies are kept right after it. */
    public Message send(String to, String subject, String body) {
        Message first = null;
        for (OutgoingMessage outgoing : policy.apply(new OutgoingMessage(to, subject, body))) {
            Message kept = sentMessages.add("EMAIL", outgoing.to(), outgoing.subject(), outgoing.body(), outgoing.status());
            if (first == null) {
                first = kept;
            }
        }
        return first;
    }
}
