package hotel.messages;

import java.util.ArrayList;
import java.util.List;

final class RuleResult {
    private final List<OutgoingMessage> additionalMessages;

    RuleResult() {
        this.additionalMessages = new ArrayList<>();
    }

    void addMessage(String to, String subject, String body) {
        additionalMessages.add(new OutgoingMessage(to, subject, body));
    }

    List<OutgoingMessage> additionalMessages() {
        return additionalMessages;
    }
}
