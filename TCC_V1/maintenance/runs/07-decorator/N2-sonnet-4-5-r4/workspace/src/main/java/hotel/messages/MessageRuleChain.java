package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Applies a sequence of rules to an outgoing message. */
public final class MessageRuleChain {

    private final List<MessageRule> rules;

    public MessageRuleChain(List<MessageRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Processes a message through all rules. Returns all messages that result from the processing
     * (the original, possibly modified or blocked, plus any additional messages like archive copies).
     */
    public List<OutgoingMessage> process(OutgoingMessage message) {
        List<OutgoingMessage> current = List.of(message);
        for (MessageRule rule : rules) {
            List<OutgoingMessage> next = new ArrayList<>();
            for (OutgoingMessage msg : current) {
                next.addAll(rule.process(msg));
            }
            current = next;
        }
        return current;
    }
}
