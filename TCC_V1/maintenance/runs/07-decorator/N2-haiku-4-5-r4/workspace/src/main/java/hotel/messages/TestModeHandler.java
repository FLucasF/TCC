package hotel.messages;

public final class TestModeHandler implements MessageHandler {
    private final boolean testMode;

    public TestModeHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void handle(MessageRequest request) {
        if (testMode && request.status() != MessageStatus.BLOCKED) {
            String newBody = "Destinatário original: " + request.originalTo() + "\n" + request.body();
            request.changeSubject("[TESTE] " + request.subject());
            request.changeBody(newBody);
            request.changeTo("testes@pousadamarealta.com.br");
        }
    }
}
