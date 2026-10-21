package hotel.messages;

final class TestModeRedirectRule implements MessageRule {

    private final boolean testMode;

    TestModeRedirectRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(ProcessedMessage message) {
        if (!testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }

        String originalRecipient = message.to();
        message.setTo("testes@pousadamarealta.com.br");
        message.setSubject("[TESTE] " + message.subject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.body());
    }
}
