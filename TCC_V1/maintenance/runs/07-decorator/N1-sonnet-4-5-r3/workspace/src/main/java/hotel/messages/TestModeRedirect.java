package hotel.messages;

import java.util.List;

/** In test mode, redirects messages to a test address instead of the real recipient. */
public final class TestModeRedirect implements SendingRule {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final boolean testMode;

    public TestModeRedirect(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (!testMode || message.status() == MessageStatus.BLOCKED) {
            return List.of(message);
        }
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();
        return List.of(message
                .withRecipient(TEST_EMAIL)
                .withSubject("[TESTE] " + message.subject())
                .withBody(newBody));
    }
}
