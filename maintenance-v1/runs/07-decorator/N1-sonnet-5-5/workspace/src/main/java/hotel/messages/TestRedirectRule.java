package hotel.messages;

import java.util.List;

/** In test mode nothing reaches a real guest: everything goes to the test mailbox. */
final class TestRedirectRule implements EmailRule {

    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public List<Outgoing> apply(Outgoing email) {
        return List.of(new Outgoing(
                TEST_ADDRESS,
                "[TESTE] " + email.subject(),
                "Destinatário original: " + email.to() + "\n" + email.body(),
                false));
    }
}
