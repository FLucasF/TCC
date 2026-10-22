package hotel.messages;

import java.util.List;
import java.util.stream.Stream;

/**
 * Sends e-mails to guests, after the rules in force have had their say. Every message, sent or blocked, is kept in
 * the list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<ScopedRule> rules;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages, List<ScopedRule> rules, boolean testMode) {
        this.sentMessages = sentMessages;
        this.rules = rules;
        this.testMode = testMode;
    }

    /** Returns the message itself; the copies the rules added are kept right after it. */
    public Message send(String to, String subject, String body) {
        List<OutgoingMessage> outgoing = List.of(new OutgoingMessage(to, subject, body));
        for (ScopedRule scoped : rules) {
            if (scoped.scope().covers(testMode)) {
                outgoing = outgoing.stream()
                        .flatMap(message -> message.isBlocked() ? Stream.of(message) : scoped.rule().apply(message).stream())
                        .toList();
            }
        }
        List<Message> kept = outgoing.stream().map(message -> sentMessages.add("EMAIL", message)).toList();
        return kept.get(0);
    }
}
