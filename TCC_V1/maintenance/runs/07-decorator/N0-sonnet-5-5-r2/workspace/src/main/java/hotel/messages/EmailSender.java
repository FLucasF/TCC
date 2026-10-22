package hotel.messages;

/**
 * Sends e-mails to guests, following the installation's {@link MessagePolicy}. Every message is kept in the list of
 * sent messages: blocked ones too (with the original address), so the front desk can call the guest.
 */
public final class EmailSender {

    private static final String CHANNEL = "EMAIL";

    private final SentMessages sentMessages;
    private final MessagePolicy policy;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, MessagePolicy.standard(false));
    }

    public EmailSender(SentMessages sentMessages, MessagePolicy policy) {
        this.sentMessages = sentMessages;
        this.policy = policy;
    }

    /** Returns the message as kept: BLOCKED, or SENT (followed in the list by its copies, if any). */
    public synchronized Message send(String to, String subject, String body) {
        Email original = new Email(to, subject, body);
        if (policy.blocks(original)) {
            return sentMessages.add(CHANNEL, to, subject, body, MessageStatus.BLOCKED);
        }
        Email email = policy.rewrite(original);
        Message sent = keep(email);
        policy.copiesOf(email).forEach(this::keep);
        return sent;
    }

    private Message keep(Email email) {
        return sentMessages.add(CHANNEL, email.to(), email.subject(), email.body(), MessageStatus.SENT);
    }
}
