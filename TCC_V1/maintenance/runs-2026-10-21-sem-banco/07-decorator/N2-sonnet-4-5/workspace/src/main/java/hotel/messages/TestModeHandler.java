package hotel.messages;

/**
 * When the system is running in test mode, redirects all messages to the test address instead of the
 * real recipient. The subject gets a [TEST] prefix, and the body starts with a line showing the
 * original recipient.
 */
final class TestModeHandler implements EmailHandler {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    TestModeHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public boolean handle(EmailContext context) {
        if (!testMode) {
            return true;
        }
        String originalRecipient = context.to;
        context.to = TEST_ADDRESS;
        context.subject = "[TESTE] " + context.subject;
        context.body = "Destinatário original: " + originalRecipient + "\n" + context.body;
        return true;
    }
}
