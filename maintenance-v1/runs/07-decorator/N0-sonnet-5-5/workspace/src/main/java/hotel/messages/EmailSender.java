package hotel.messages;

import java.util.List;

/**
 * Sends e-mails to guests, applying the inn's {@link MessageRule}s. Every e-mail, sent or blocked, is kept in the
 * list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, false);
    }

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.testMode = testMode;
        this.rules = MessageRules.standard().stream().filter(rule -> rule.scope().appliesTo(testMode)).toList();
    }

    /** Returns the record of the message asked for: SENT, or BLOCKED if a rule stopped it. */
    public synchronized Message send(String to, String subject, String body) {
        Outgoing requested = new Outgoing(to, subject, body);
        if (rules.stream().anyMatch(rule -> rule.blocks(requested))) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }
        Outgoing outgoing = requested;
        for (MessageRule rule : rules) {
            outgoing = rule.rewrite(outgoing);
        }
        Message message = record(outgoing);
        for (MessageRule rule : rules) {
            rule.copies(outgoing).forEach(this::record);
        }
        return message;
    }

    private Message record(Outgoing outgoing) {
        return sentMessages.add("EMAIL", outgoing.to(), outgoing.subject(), outgoing.body(), MessageStatus.SENT);
    }
}
