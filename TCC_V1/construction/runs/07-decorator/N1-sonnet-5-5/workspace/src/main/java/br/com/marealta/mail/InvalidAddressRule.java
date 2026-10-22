package br.com.marealta.mail;

import java.util.List;

/** Bloqueia a mensagem cujo endereço é inválido. */
public final class InvalidAddressRule implements MailRule {
    @Override
    public List<Message> apply(Message message) {
        return List.of(EmailAddress.isValid(message.to()) ? message : message.blocked());
    }
}
