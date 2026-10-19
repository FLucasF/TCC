package br.com.marealta.messaging;

import java.util.ArrayList;
import java.util.List;

/** Toda mensagem enviada gera uma cópia para o arquivo, logo depois da original. */
public class ArchiveCopyRule implements DispatchRule {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<Message> apply(Message message, Next next) {
        List<Message> result = new ArrayList<>(next.send(message));
        if (result.stream().anyMatch(Message::isSent)) {
            result.add(Message.draft(message.channel(), ARCHIVE_ADDRESS,
                    "[Cópia] " + message.subject(), message.body()));
        }
        return result;
    }
}
