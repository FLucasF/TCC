package hotel.messages;

import java.util.Map;
import java.util.Optional;

/**
 * Sends e-mails to guests, under the installation's {@link SendingRules}. Every e-mail is kept in the list of
 * sent messages: the ones that went out as SENT, the ones the rules stopped as BLOCKED (with the original address).
 */
public final class EmailSender {

    private static final String CHANNEL = "EMAIL";

    private final SentMessages sentMessages;
    private final SendingRules rules;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, SendingRules.from(Map.of()));
    }

    public EmailSender(SentMessages sentMessages, SendingRules rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    /** Returns the message that was kept for {@code to}: SENT, or BLOCKED when a rule stopped it. */
    public synchronized Message send(String to, String subject, String body) {
        Draft original = new Draft(to, subject, body);
        Optional<String> reason = rules.blockReason(original);
        if (reason.isPresent()) {
            return sentMessages.add(CHANNEL, to, subject, body, MessageStatus.BLOCKED);
        }
        Draft out = rules.rewrite(original);
        Message sent = sentMessages.add(CHANNEL, out.to(), out.subject(), out.body(), MessageStatus.SENT);
        for (Draft copy : rules.copiesOf(out)) {
            sentMessages.add(CHANNEL, copy.to(), copy.subject(), copy.body(), MessageStatus.SENT);
        }
        return sent;
    }
}
