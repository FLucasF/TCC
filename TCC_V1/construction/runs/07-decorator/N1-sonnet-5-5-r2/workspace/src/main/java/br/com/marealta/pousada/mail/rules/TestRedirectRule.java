package br.com.marealta.pousada.mail.rules;

import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.mail.MessageRule;
import java.util.List;

/** Desvia a mensagem para o endereço de testes, anotando para quem ela iria. */
public class TestRedirectRule implements MessageRule {
    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        return List.of(message.with(
                testAddress,
                "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
