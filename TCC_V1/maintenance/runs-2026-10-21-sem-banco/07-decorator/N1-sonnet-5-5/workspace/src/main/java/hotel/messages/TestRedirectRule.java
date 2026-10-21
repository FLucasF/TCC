package hotel.messages;

import java.util.List;

/** In test mode nothing reaches the real address: it all goes to the test mailbox. */
public final class TestRedirectRule implements MessageRule {

    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public List<Draft> apply(Draft draft) {
        return List.of(new Draft(
                testAddress,
                "[TESTE] " + draft.subject(),
                "Destinatário original: " + draft.to() + "\n" + draft.body(),
                draft.status()));
    }
}
