package hotel.messages;

/** A rule together with where it is in force in one installation. */
public record ScopedRule(RuleScope scope, MessageRule rule) {
}
