package br.com.marealta.message;

/** Desvia a mensagem para o endereço de testes, guardando o destinatário original no texto. */
public class TestRedirectRule implements MessageRule {

    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public String name() {
        return "test-redirect";
    }

    @Override
    public void apply(Delivery delivery) {
        delivery.redirect(testAddress,
                "[TESTE] " + delivery.subject(),
                "Destinatário original: " + delivery.to() + "\n" + delivery.body());
    }
}
