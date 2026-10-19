package br.com.marealta.messaging;

import java.util.List;

/** Modo de teste: nada sai para o endereço real. */
public class TestRedirectRule implements DispatchRule {
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    @Override
    public List<Message> apply(Message message, Next next) {
        return next.send(message.redirected(
                TEST_ADDRESS,
                "[TESTE] " + message.subject(),
                "Destinatário original: " + message.to() + "\n" + message.body()));
    }
}
