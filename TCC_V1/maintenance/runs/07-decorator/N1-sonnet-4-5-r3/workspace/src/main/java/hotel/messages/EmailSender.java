package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<SendingRule> rules;

    public EmailSender(SentMessages sentMessages, List<SendingRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    public Message send(String to, String subject, String body) {
        List<OutgoingMessage> messages = List.of(new OutgoingMessage(to, subject, body));
        for (SendingRule rule : rules) {
            List<OutgoingMessage> processed = new ArrayList<>();
            for (OutgoingMessage message : messages) {
                processed.addAll(rule.apply(message));
            }
            messages = processed;
        }
        Message firstMessage = null;
        for (OutgoingMessage message : messages) {
            Message saved = sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), message.status());
            if (firstMessage == null) {
                firstMessage = saved;
            }
        }
        return firstMessage;
    }
}
