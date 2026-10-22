package br.com.marealta.mail;

import java.util.List;

/** Desvia a mensagem para um endereço de teste, anotando o destinatário original. */
public final class TestRedirectRule implements MailRule {
    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        return List.of(message
                .withTo(testAddress)
                .withSubject("[TESTE] " + message.subject())
                .withBody("Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
