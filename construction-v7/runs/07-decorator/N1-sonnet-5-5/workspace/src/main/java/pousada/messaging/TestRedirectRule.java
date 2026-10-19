package pousada.messaging;

import java.util.List;

/** No modo de teste, desvia tudo para o endereço de testes, registrando o destinatário original. */
public class TestRedirectRule implements MessageRule {
    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public RuleScope scope() {
        return RuleScope.TEST_ONLY;
    }

    @Override
    public List<Message> apply(Message message) {
        return List.of(message
                .withBody("Destinatário original: " + message.to() + "\n" + message.body())
                .withSubject("[TESTE] " + message.subject())
                .withTo(testAddress));
    }
}
