package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages, List<MessageRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    public Message send(String to, String subject, String body) {
        List<OutgoingMessage> messages = new ArrayList<>();
        messages.add(new OutgoingMessage(to, subject, body));

        for (MessageRule rule : rules) {
            List<OutgoingMessage> updated = new ArrayList<>();
            for (OutgoingMessage message : messages) {
                updated.addAll(rule.apply(message));
            }
            messages = updated;
        }

        Message firstMessage = null;
        for (OutgoingMessage message : messages) {
            Message persisted = sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), message.status());
            if (firstMessage == null) {
                firstMessage = persisted;
            }
        }
        return firstMessage;
    }
}
