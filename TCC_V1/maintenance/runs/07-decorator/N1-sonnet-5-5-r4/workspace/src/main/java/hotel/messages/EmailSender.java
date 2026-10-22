package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends e-mails to guests, after the rules have had their say. Every e-mail, sent or blocked, is kept in the
 * list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, List.of());
    }

    public EmailSender(SentMessages sentMessages, List<MessageRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    /** Returns the message the caller asked for; the ones the rules added come right after it in the list. */
    public Message send(String to, String subject, String body) {
        List<Draft> drafts = List.of(Draft.email(to, subject, body));
        for (MessageRule rule : rules) {
            List<Draft> next = new ArrayList<>();
            for (Draft draft : drafts) {
                next.addAll(draft.isBlocked() ? List.of(draft) : rule.apply(draft));
            }
            drafts = next;
        }
        Message first = null;
        for (Draft draft : drafts) {
            Message kept = sentMessages.add("EMAIL", draft.to(), draft.subject(), draft.body(), draft.status());
            if (first == null) {
                first = kept;
            }
        }
        return first;
    }
}
