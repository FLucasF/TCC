package hotel.messages;

public final class ArchiveCopyRule implements EmailRule {
    private final boolean testModeEnabled;

    public ArchiveCopyRule(boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    @Override
    public EmailRuleResult apply(String to, String subject, String body) {
        if (testModeEnabled) {
            return new EmailRuleResult(to, subject, body);
        }
        String newSubject = "[Cópia] " + subject;
        return new EmailRuleResult(to, subject, body).withAdditionalMessage(createArchiveMessage(to, newSubject, body));
    }

    private String createArchiveMessage(String originalTo, String newSubject, String body) {
        return "EMAIL|arquivo@pousadamarealta.com.br|" + newSubject + "|" + body + "|" + originalTo;
    }
}
