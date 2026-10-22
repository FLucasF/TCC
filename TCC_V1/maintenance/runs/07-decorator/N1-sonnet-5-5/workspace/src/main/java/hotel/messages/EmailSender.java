package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends e-mails to guests, after the inn's message rules. Every message that comes out of the rules is kept in the
 * list of sent messages, blocked ones included.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages, List<MessageRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    /** Returns the first message kept: the one for the guest (or its replacement), not the extra copies. */
    public Message send(String to, String subject, String body) {
        List<Outgoing> outgoing = List.of(Outgoing.toSend(to, subject, body));
        for (MessageRule rule : rules) {
            List<Outgoing> next = new ArrayList<>();
            for (Outgoing message : outgoing) {
                if (message.isBlocked()) {
                    next.add(message);
                } else {
                    next.addAll(rule.apply(message));
                }
            }
            outgoing = next;
        }
        Message first = null;
        for (Outgoing message : outgoing) {
            Message kept = sentMessages.add("EMAIL", message);
            if (first == null) {
                first = kept;
            }
        }
        return first;
    }
}
