package br.com.marealta.pousada.mail.rules;

import br.com.marealta.pousada.mail.Message;
import br.com.marealta.pousada.mail.MessageRule;
import java.util.List;

/** Bloqueia mensagem para endereço inválido: um único @, algo antes dele e um ponto no meio do nome depois. */
public class ValidAddressRule implements MessageRule {

    @Override
    public List<Message> apply(Message message) {
        return List.of(isValid(message.to()) ? message : message.blocked());
    }

    static boolean isValid(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) {
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
