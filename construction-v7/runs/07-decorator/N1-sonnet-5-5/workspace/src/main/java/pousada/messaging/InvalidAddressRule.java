package pousada.messaging;

import java.util.List;

/** Bloqueia mensagem para endereço inválido. */
public class InvalidAddressRule implements MessageRule {
    @Override
    public RuleScope scope() {
        return RuleScope.ALWAYS;
    }

    @Override
    public List<Message> apply(Message message) {
        return List.of(isValid(message.to()) ? message : message.withStatus(MessageStatus.BLOCKED));
    }

    /** Um único @, algo antes dele e, depois, um nome com ponto no meio. */
    static boolean isValid(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) {
            return false;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at != address.lastIndexOf('@')) {
            return false;
        }
        String domain = address.substring(at + 1);
        return domain.contains(".") && !domain.startsWith(".") && !domain.endsWith(".") && !domain.contains("..");
    }
}
