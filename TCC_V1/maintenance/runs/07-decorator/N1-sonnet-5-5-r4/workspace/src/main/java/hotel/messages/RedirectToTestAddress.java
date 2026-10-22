package hotel.messages;

import java.util.List;

/** Sends the message to the inn's test address, saying who it was meant for. */
public final class RedirectToTestAddress implements MessageRule {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public List<Draft> apply(Draft draft) {
        return List.of(new Draft(
                TEST_ADDRESS,
                "[TESTE] " + draft.subject(),
                "Destinatário original: " + draft.to() + "\n" + draft.body(),
                draft.status()));
    }
}
