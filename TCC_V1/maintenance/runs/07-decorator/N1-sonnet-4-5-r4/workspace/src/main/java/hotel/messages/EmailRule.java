package hotel.messages;

interface EmailRule {
    void apply(OutgoingMessage message, RuleResult result);
}
