package hotel.messages;

/** In test mode, redirects all messages to the test address. */
public final class TestModeHandler implements MessageHandler {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    public TestModeHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public ProcessedMessage handle(OutgoingMessage message) {
        if (!testMode) {
            return ProcessedMessage.sent(message.to(), message.subject(), message.body());
        }

        String newSubject = "[TESTE] " + message.subject();
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();

        return ProcessedMessage.sent(TEST_ADDRESS, newSubject, newBody);
    }
}
