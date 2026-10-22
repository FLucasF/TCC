package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class EmailProcessor {
    private final List<EmailRule> rules;

    public EmailProcessor(List<EmailRule> rules) {
        this.rules = rules;
    }

    public ProcessedEmail process(String to, String subject, String body) {
        List<ProcessedEmail> additionalEmails = new ArrayList<>();
        String currentTo = to;
        String currentSubject = subject;
        String currentBody = body;
        String status = "SENT";

        for (EmailRule rule : rules) {
            EmailRuleResult result = rule.apply(currentTo, currentSubject, currentBody);
            if (result.blocked()) {
                status = "BLOCKED";
                currentTo = to;
                currentSubject = subject;
                currentBody = body;
                break;
            }
            currentTo = result.to();
            currentSubject = result.subject();
            currentBody = result.body();
            for (String additionalMessage : result.additionalMessages()) {
                additionalEmails.add(parseAdditionalMessage(additionalMessage));
            }
        }

        return new ProcessedEmail(currentTo, currentSubject, currentBody, status, additionalEmails);
    }

    private static ProcessedEmail parseAdditionalMessage(String message) {
        String[] parts = message.split("\\|", 5);
        return new ProcessedEmail(parts[1], parts[2], parts[3], "SENT", List.of());
    }

    public record ProcessedEmail(String to, String subject, String body, String status, List<ProcessedEmail> additionalEmails) {}
}
