package hotel.messages;

import java.util.List;

public final class TestModeRule implements MessageRule {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final String TEST_PREFIX = "[TESTE] ";

    private final boolean isTestMode;

    public TestModeRule(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (!isTestMode || message.status() == MessageStatus.BLOCKED) {
            return List.of(message);
        }
        String originalRecipient = message.to();
        message.setTo(TEST_EMAIL);
        message.setSubject(TEST_PREFIX + message.subject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.body());
        return List.of(message);
    }
}
