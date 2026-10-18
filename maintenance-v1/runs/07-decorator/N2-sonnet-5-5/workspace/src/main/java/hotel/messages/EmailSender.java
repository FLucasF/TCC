package hotel.messages;

import java.util.List;

/**
 * Sends e-mails to guests, applying the inn's {@link MessageRules}. Every message handled, sent or blocked, is
 * kept in the list of sent messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<ScopedRule> rules;

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this(sentMessages, testMode, MessageRules.standard());
    }

    EmailSender(SentMessages sentMessages, boolean testMode, List<ScopedRule> allRules) {
        this.sentMessages = sentMessages;
        this.rules = allRules.stream().filter(r -> r.scope().appliesIn(testMode)).toList();
    }

    public Message send(String to, String subject, String body) {
        OutgoingEmail email = new OutgoingEmail(to, subject, body);
        for (ScopedRule scoped : rules) {
            scoped.rule().apply(email);
            if (email.blocked()) {
                return sentMessages.add("EMAIL", email.originalTo(), subject, body, MessageStatus.BLOCKED);
            }
        }
        Message sent = sentMessages.add("EMAIL", email.to(), email.subject(), email.body(), MessageStatus.SENT);
        for (OutgoingEmail copy : email.copies()) {
            sentMessages.add("EMAIL", copy.to(), copy.subject(), copy.body(), MessageStatus.SENT);
        }
        return sent;
    }
}
