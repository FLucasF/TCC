package hotel.messages;

import java.util.List;

/**
 * Sends e-mails to guests. The rules of the installation decide what happens to each one, and every e-mail, sent or
 * blocked, is kept in the list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;
    private final List<ScopedRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, false, List.of());
    }

    public EmailSender(SentMessages sentMessages, boolean testMode, List<ScopedRule> rules) {
        this.sentMessages = sentMessages;
        this.testMode = testMode;
        this.rules = rules;
    }

    public Message send(String to, String subject, String body) {
        Draft draft = new Draft(to, subject, body);
        for (ScopedRule scoped : rules) {
            if (draft.blocked()) {
                break;
            }
            if (scoped.scope().covers(testMode)) {
                draft = scoped.rule().apply(draft);
            }
        }
        if (draft.blocked()) {
            return sentMessages.add("EMAIL", to, subject, body, MessageStatus.BLOCKED);
        }
        Message message = sentMessages.add("EMAIL", draft.to(), draft.subject(), draft.body(), MessageStatus.SENT);
        for (Draft copy : draft.copies()) {
            sentMessages.add("EMAIL", copy.to(), copy.subject(), copy.body(), MessageStatus.SENT);
        }
        return message;
    }
}
