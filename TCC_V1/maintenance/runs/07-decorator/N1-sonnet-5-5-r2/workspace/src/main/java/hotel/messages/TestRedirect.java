package hotel.messages;

/** Sends the message to the test address instead of the real one, saying who it was meant for. */
final class TestRedirect implements MessageRule {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public Draft apply(Draft draft) {
        if (draft.isBlocked()) {
            return draft;
        }
        return new Draft(
                TEST_ADDRESS,
                "[TESTE] " + draft.subject(),
                "Destinatário original: " + draft.to() + "\n" + draft.body());
    }
}
