package br.com.marealta.pousada.messaging;

import java.util.List;

/** Bloqueia mensagem para endereço inválido: um único @, algo antes dele e um domínio com ponto no meio. */
public final class ValidAddressPolicy implements MessagePolicy {

    @Override
    public List<Message> apply(Message message) {
        if (message.isSent() && !isValid(message.to())) {
            return List.of(message.blocked());
        }
        return List.of(message);
    }

    static boolean isValid(String address) {
        if (address == null) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.length() > 2 && domain.substring(1, domain.length() - 1).contains(".");
    }
}
