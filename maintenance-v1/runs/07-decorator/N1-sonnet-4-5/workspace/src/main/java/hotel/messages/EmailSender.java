package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final boolean testMode;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
        this.testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        this.rules = List.of(
                new EmailValidationRule(),
                new TestModeRedirectRule(),
                new ArchiveCopyRule()
        );
    }

    public Message send(String to, String subject, String body) {
        MessageToSend message = new MessageToSend(to, subject, body, MessageStatus.SENT);

        for (MessageRule rule : rules) {
            message = rule.apply(message, testMode);
        }

        Message mainMessage = sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), message.status());

        for (MessageToSend additional : message.additionalMessages()) {
            sentMessages.add("EMAIL", additional.to(), additional.subject(), additional.body(), additional.status());
        }

        return mainMessage;
    }
}
