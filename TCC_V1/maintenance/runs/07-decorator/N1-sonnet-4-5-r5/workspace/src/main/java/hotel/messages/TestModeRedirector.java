package hotel.messages;

/** Redireciona mensagens para endereço de testes quando o modo de teste está ativo. */
public final class TestModeRedirector implements MessageProcessor {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private final boolean testMode;

    public TestModeRedirector(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void process(OutgoingMessage message) {
        if (!testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }
        String originalRecipient = message.to();
        message.setTo(TEST_EMAIL);
        message.setSubject("[TESTE] " + message.subject());
        message.setBody("Destinatário original: " + originalRecipient + "\n" + message.body());
    }
}
