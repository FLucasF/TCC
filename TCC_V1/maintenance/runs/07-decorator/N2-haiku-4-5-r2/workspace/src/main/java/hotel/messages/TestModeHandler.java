package hotel.messages;

import java.util.List;

/** In test mode, redirects messages to a test address. */
public final class TestModeHandler implements MessageHandler {

    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    @Override
    public List<MessageToStore> handle(MessageToStore message) {
        String newBody = "Destinatário original: " + message.to() + "\n" + message.body();
        return List.of(new MessageToStore(TEST_EMAIL, "[TESTE] " + message.subject(), newBody, "SENT"));
    }
}
