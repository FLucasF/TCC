package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailRule> rules;

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.rules = List.of(
                new EmailValidationRule(),
                new TestModeRule(testMode),
                new ArchiveCopyRule(testMode)
        );
    }

    public Message send(String to, String subject, String body) {
        OutboundMessage message = new OutboundMessage(to, subject, body);
        for (EmailRule rule : rules) {
            rule.apply(message);
        }
        Message primary = sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), message.status());
        for (OutboundMessage copy : message.copies()) {
            sentMessages.add("EMAIL", copy.to(), copy.subject(), copy.body(), copy.status());
        }
        return primary;
    }
}
