package br.com.pousadamarealta.reservas.messaging;

/** Modo de teste: nada sai para o endereço real. */
public final class TestRedirectRule implements MessageRule {

    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public void apply(Draft draft) {
        draft.redirect(testAddress,
                "[TESTE] " + draft.subject(),
                "Destinatário original: " + draft.to() + "\n" + draft.body());
    }
}
