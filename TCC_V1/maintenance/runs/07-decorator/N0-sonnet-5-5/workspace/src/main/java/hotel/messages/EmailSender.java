package hotel.messages;

import java.util.List;
import java.util.Optional;

/**
 * Sends e-mails to guests, applying the installation's rules. Every e-mail handled, sent or blocked, is kept
 * in the list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<SendRule> rules;

    /** A sender with no rules: everything goes out as asked. */
    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, List.of());
    }

    public EmailSender(SentMessages sentMessages, List<SendRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = List.copyOf(rules);
    }

    public synchronized Message send(String to, String subject, String body) {
        Outgoing message = Outgoing.of(to, subject, body);
        for (SendRule rule : rules) {
            if (rule.blockReason(message).isPresent()) {
                return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
            }
        }
        for (SendRule rule : rules) {
            message = rule.rewrite(message);
        }
        Message result = record(message);
        for (SendRule rule : rules) {
            rule.sent(message);
        }
        for (SendRule rule : rules) {
            for (Outgoing copy : rule.copiesOf(message)) {
                record(copy);
            }
        }
        return result;
    }

    private Message record(Outgoing message) {
        return sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), MessageStatus.SENT);
    }
}
