package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends e-mails to guests, after the rules of the installation have had their say. Every e-mail, sent or
 * blocked, is kept in the list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    /** A sender without rules: every e-mail goes out as it is. */
    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, List.of(), false);
    }

    public EmailSender(SentMessages sentMessages, List<ScopedRule> rules, boolean testMode) {
        this.sentMessages = sentMessages;
        this.rules = rules.stream()
                .filter(scoped -> scoped.scope().appliesTo(testMode))
                .map(ScopedRule::rule)
                .toList();
    }

    /** Returns the message that was asked for (the first one recorded), whether it was sent or blocked. */
    public synchronized Message send(String to, String subject, String body) {
        List<Draft> drafts = List.of(Draft.of(to, subject, body));
        for (MessageRule rule : rules) {
            List<Draft> next = new ArrayList<>();
            for (Draft draft : drafts) {
                if (draft.blocked()) {
                    next.add(draft);
                } else {
                    next.addAll(rule.apply(draft));
                }
            }
            drafts = next;
        }
        Message first = null;
        for (Draft draft : drafts) {
            Message message = sentMessages.add("EMAIL", draft.to(), draft.subject(), draft.body(), draft.status());
            if (first == null) {
                first = message;
            }
        }
        return first;
    }
}
