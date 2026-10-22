package hotel.messages;

public final class ValidationRule implements EmailRule {
    @Override
    public EmailRuleResult apply(String to, String subject, String body) {
        if (EmailValidator.isValid(to)) {
            return new EmailRuleResult(to, subject, body);
        }
        return EmailRuleResult.blockedResult();
    }
}
