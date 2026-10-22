package hotel.messages;

import java.util.List;

/** In test mode nothing goes to the real address: it goes to the test mailbox, saying who it was meant for. */
public final class TestRedirectRule implements MessageRule {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        return List.of(message
                .withTo(TEST_ADDRESS)
                .withSubject("[TESTE] " + message.subject())
                .withBody("Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
