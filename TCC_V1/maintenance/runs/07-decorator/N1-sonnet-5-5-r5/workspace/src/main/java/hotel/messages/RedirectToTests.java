package hotel.messages;

/** Nothing goes to the real address: it goes to the test mailbox, which says who would have received it. */
public final class RedirectToTests implements MessageRule {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public Draft apply(Draft draft) {
        return draft.withTo(TEST_ADDRESS)
                .withSubject("[TESTE] " + draft.subject())
                .withBody("Destinatário original: " + draft.to() + "\n" + draft.body());
    }
}
