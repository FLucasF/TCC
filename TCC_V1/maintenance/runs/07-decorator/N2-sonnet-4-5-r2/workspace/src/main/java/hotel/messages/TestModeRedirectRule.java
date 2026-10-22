package hotel.messages;

/** In test mode, redirects messages to the test address with the original recipient noted. */
final class TestModeRedirectRule implements MessageRule {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    TestModeRedirectRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(MessageContext context) {
        if (!testMode || context.isBlocked()) {
            return;
        }
        OutgoingMessage original = context.message();
        String newSubject = "[TESTE] " + original.subject();
        String newBody = "Destinatário original: " + original.to() + "\n" + original.body();
        context.transformMessage(new OutgoingMessage(TEST_ADDRESS, newSubject, newBody));
    }
}
