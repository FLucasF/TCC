package hotel.messages;

import java.util.List;

/** In test mode, redirects messages to the test address instead of the real recipient. */
public final class TestModeRedirectRule implements MessageRule {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    public TestModeRedirectRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<OutgoingMessage> process(OutgoingMessage message) {
        if (testMode && !message.isBlocked()) {
            String originalRecipient = message.to();
            message.changeTo(TEST_ADDRESS);
            message.changeSubject("[TESTE] " + message.subject());
            message.changeBody("Destinatário original: " + originalRecipient + "\n" + message.body());
        }
        return List.of(message);
    }
}
