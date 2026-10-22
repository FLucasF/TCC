package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public record EmailRuleResult(String to, String subject, String body, boolean blocked, List<String> additionalMessages) {
    public EmailRuleResult(String to, String subject, String body) {
        this(to, subject, body, false, List.of());
    }

    public static EmailRuleResult blockedResult() {
        return new EmailRuleResult("", "", "", true, List.of());
    }

    public EmailRuleResult withAdditionalMessage(String message) {
        List<String> messages = new ArrayList<>(additionalMessages);
        messages.add(message);
        return new EmailRuleResult(to, subject, body, blocked, messages);
    }
}
