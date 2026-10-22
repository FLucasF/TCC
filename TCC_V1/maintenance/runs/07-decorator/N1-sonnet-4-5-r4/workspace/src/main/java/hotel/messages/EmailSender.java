package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, buildRules());
    }

    EmailSender(SentMessages sentMessages, List<EmailRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    public Message send(String to, String subject, String body) {
        OutgoingMessage message = new OutgoingMessage(to, subject, body);
        RuleResult result = new RuleResult();

        for (EmailRule rule : rules) {
            rule.apply(message, result);
        }

        Message sent = sentMessages.add("EMAIL", message.to(), message.subject(), message.body(), message.status());

        for (OutgoingMessage additional : result.additionalMessages()) {
            sentMessages.add("EMAIL", additional.to(), additional.subject(), additional.body(), additional.status());
        }

        return sent;
    }

    private static List<EmailRule> buildRules() {
        List<EmailRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());

        boolean testMode = "true".equals(System.getenv("HOTEL_TEST_MODE"));
        if (testMode) {
            rules.add(new TestModeRule());
        } else {
            rules.add(new ArchiveCopyRule());
        }

        return rules;
    }
}
