package br.com.marealta.pousada.messaging;

import java.util.List;

/** Em vez de ir ao endereço real, a mensagem vai para um endereço de testes, avisando quem seria o destinatário. */
public final class TestRedirectPolicy implements MessagePolicy {
    private final String testAddress;

    public TestRedirectPolicy(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        if (!message.isSent()) {
            return List.of(message);
        }
        return List.of(message.redirectedTo(
                testAddress,
                "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
