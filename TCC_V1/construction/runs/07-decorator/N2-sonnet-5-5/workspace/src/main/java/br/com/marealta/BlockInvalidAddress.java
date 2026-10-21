package br.com.marealta;

/** Não envia para endereço inválido: a mensagem fica na lista como bloqueada. */
public class BlockInvalidAddress implements MessageSender {
    private final MessageSender next;
    private final Outbox outbox;

    public BlockInvalidAddress(MessageSender next, Outbox outbox) {
        this.next = next;
        this.outbox = outbox;
    }

    @Override
    public void send(OutgoingMessage message) {
        if (isValid(message.to())) {
            next.send(message);
        } else {
            outbox.block(message);
        }
    }

    /** Um único @, algo antes dele e, depois, um nome com ponto no meio. */
    static boolean isValid(String address) {
        if (address == null || address.chars().anyMatch(Character::isWhitespace)) return false;
        int at = address.indexOf('@');
        if (at < 1 || at != address.lastIndexOf('@')) return false;
        String domain = address.substring(at + 1);
        return domain.indexOf('.') > 0 && !domain.endsWith(".");
    }
}
