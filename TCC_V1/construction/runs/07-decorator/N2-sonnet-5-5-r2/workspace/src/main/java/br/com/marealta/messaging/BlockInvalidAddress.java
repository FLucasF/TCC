package br.com.marealta.messaging;

import java.util.regex.Pattern;

/** Não envia para endereço inválido: registra como bloqueada, com o endereço original. */
public class BlockInvalidAddress implements MessageSender {
    // um único @, algo antes dele e, depois, um nome com pelo menos um ponto no meio
    private static final Pattern VALID = Pattern.compile("[^@\\s]+@[^@.\\s]+(\\.[^@.\\s]+)+");

    private final MessageSender next;
    private final MessageLog log;

    public BlockInvalidAddress(MessageSender next, MessageLog log) {
        this.next = next;
        this.log = log;
    }

    static boolean isValid(String address) {
        return address != null && VALID.matcher(address).matches();
    }

    @Override
    public MessageStatus send(Message message) {
        if (!isValid(message.to())) {
            log.save(message, MessageStatus.BLOCKED);
            return MessageStatus.BLOCKED;
        }
        return next.send(message);
    }
}
