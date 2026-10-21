package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRedirectRule(testMode));
        rules.add(new ArchiveCopyRule(testMode));
    }

    public Message send(String to, String subject, String body) {
        ProcessedMessage processed = new ProcessedMessage(to, subject, body);

        for (MessageRule rule : rules) {
            rule.apply(processed);
        }

        Message mainMessage = sentMessages.add(
            "EMAIL",
            processed.to(),
            processed.subject(),
            processed.body(),
            processed.status()
        );

        for (ProcessedMessage derived : processed.derived()) {
            sentMessages.add(
                "EMAIL",
                derived.to(),
                derived.subject(),
                derived.body(),
                derived.status()
            );
        }

        return mainMessage;
    }
}
