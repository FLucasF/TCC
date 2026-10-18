package hotel.messages;

import java.util.List;
import java.util.stream.Stream;

/** Sends e-mails to guests. Every e-mail handled, sent or blocked, is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailRule> rules;

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.rules = EmailRules.forMode(testMode);
    }

    /** Returns the message for the original e-mail; extra e-mails the rules add are recorded after it. */
    public synchronized Message send(String to, String subject, String body) {
        List<Outgoing> emails = List.of(new Outgoing(to, subject, body, false));
        for (EmailRule rule : rules) {
            emails = emails.stream()
                    .flatMap(email -> email.blocked() ? Stream.of(email) : rule.apply(email).stream())
                    .toList();
        }
        Message first = null;
        for (Outgoing email : emails) {
            Message message = sentMessages.add(
                    "EMAIL",
                    email.to(),
                    email.subject(),
                    email.body(),
                    email.blocked() ? MessageStatus.BLOCKED : MessageStatus.SENT);
            if (first == null) {
                first = message;
            }
        }
        return first;
    }
}
