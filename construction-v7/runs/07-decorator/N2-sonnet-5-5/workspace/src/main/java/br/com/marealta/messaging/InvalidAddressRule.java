package br.com.marealta.messaging;

import java.util.List;

/** Endereço inválido: a mensagem não sai e fica na lista como bloqueada, com o endereço original. */
public class InvalidAddressRule implements DispatchRule {
    @Override
    public List<Message> apply(Message message, Next next) {
        if (EmailAddress.isValid(message.to())) return next.send(message);
        return List.of(message.blocked());
    }
}
