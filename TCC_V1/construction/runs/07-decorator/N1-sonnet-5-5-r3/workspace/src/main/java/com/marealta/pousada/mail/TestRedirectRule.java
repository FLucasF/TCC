package com.marealta.pousada.mail;

import java.util.List;

/** Desvia a mensagem para o endereço de testes, avisando no texto quem seria o destinatário. */
public final class TestRedirectRule implements MailRule {
    private final String testAddress;

    public TestRedirectRule(String testAddress) {
        this.testAddress = testAddress;
    }

    @Override
    public List<Message> apply(Message message) {
        return List.of(message.redirectedTo(testAddress, "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
