package hotel.messages;

public final class TestModeRule implements EmailRule {

    private final boolean testMode;

    public TestModeRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(ProcessingResult result) {
        if (!testMode) {
            return;
        }
        OutgoingMessage message = result.original();
        if (message.status() == MessageStatus.BLOCKED) {
            return;
        }
        String originalRecipient = message.to();
        String newBody = "Destinatário original: " + originalRecipient + "\n" + message.body();
        String newSubject = "[TESTE] " + message.subject();
        result.setOriginal(message
                .withRecipient("testes@pousadamarealta.com.br")
                .withSubject(newSubject)
                .withBody(newBody));
    }
}
