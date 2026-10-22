package hotel.messages;

public final class TestModeRule implements EmailRule {
    private final boolean testModeEnabled;

    public TestModeRule(boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    @Override
    public EmailRuleResult apply(String to, String subject, String body) {
        if (!testModeEnabled) {
            return new EmailRuleResult(to, subject, body);
        }
        String newSubject = "[TESTE] " + subject;
        String newBody = "Destinatário original: " + to + "\n" + body;
        return new EmailRuleResult("testes@pousadamarealta.com.br", newSubject, newBody);
    }
}
