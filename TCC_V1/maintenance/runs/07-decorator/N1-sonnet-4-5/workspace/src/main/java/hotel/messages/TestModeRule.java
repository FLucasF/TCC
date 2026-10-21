package hotel.messages;

/** Redirects messages to the test address when in test mode. */
final class TestModeRule implements EmailRule {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    TestModeRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(OutboundMessage message) {
        if (!testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }
        String originalRecipient = message.to();
        message.setTo(TEST_ADDRESS);
        message.setSubject("[TESTE] " + message.subject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.body());
    }
}
