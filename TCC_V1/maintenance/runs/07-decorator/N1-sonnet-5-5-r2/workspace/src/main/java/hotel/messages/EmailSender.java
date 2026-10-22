package hotel.messages;

import java.util.List;

/**
 * Sends e-mails to guests, following the rules of the installation. Every e-mail, sent or blocked, is kept in the
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

    public Message send(String to, String subject, String body) {
        Draft draft = new Draft(to, subject, body);
        for (MessageRule rule : rules) {
            draft = rule.apply(draft);
        }
        Message message = keep(draft);
        if (!draft.isBlocked()) {
            for (MessageRule rule : rules) {
                rule.extrasFor(draft).forEach(this::keep);
            }
        }
        return message;
    }

    private Message keep(Draft draft) {
        return sentMessages.add("EMAIL", draft);
    }
}
