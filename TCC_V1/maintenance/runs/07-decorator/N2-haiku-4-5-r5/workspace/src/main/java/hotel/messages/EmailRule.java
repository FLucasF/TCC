package hotel.messages;

public interface EmailRule {
    EmailRuleResult apply(String to, String subject, String body);
}
