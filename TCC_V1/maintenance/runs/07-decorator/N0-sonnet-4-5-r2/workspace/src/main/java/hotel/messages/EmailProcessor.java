package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class EmailProcessor {

    private final List<EmailRule> rules;

    public EmailProcessor(boolean testMode) {
        this.rules = new ArrayList<>();
        rules.add(new EmailValidationRule());
        rules.add(new TestModeRule(testMode));
        rules.add(new ArchiveCopyRule(testMode));
    }

    public ProcessingResult process(String to, String subject, String body) {
        OutgoingMessage message = OutgoingMessage.create(to, subject, body);
        ProcessingResult result = new ProcessingResult(message);
        for (EmailRule rule : rules) {
            rule.apply(result);
        }
        return result;
    }
}
