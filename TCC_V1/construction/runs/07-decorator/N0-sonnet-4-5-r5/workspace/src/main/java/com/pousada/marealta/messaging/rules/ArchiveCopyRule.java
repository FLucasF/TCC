package com.pousada.marealta.messaging.rules;

import com.pousada.marealta.domain.Message;
import com.pousada.marealta.domain.MessageStatus;
import com.pousada.marealta.messaging.MessageRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Regra que cria cópia de mensagens para arquivo.
 *
 * Para cada mensagem enviada (não bloqueada):
 * - Cria cópia para arquivo@pousadamarealta.com.br
 * - Assunto recebe prefixo "[Cópia] "
 * - Corpo permanece o mesmo
 */
public class ArchiveCopyRule implements MessageRule {
    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    private final boolean enabled;

    public ArchiveCopyRule(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public List<Message> process(Message message) {
        if (!enabled) {
            return Collections.singletonList(message);
        }

        // Não cria cópia de mensagens bloqueadas
        if (message.getStatus() == MessageStatus.BLOCKED) {
            return Collections.singletonList(message);
        }

        List<Message> messages = new ArrayList<>();
        messages.add(message);

        // Cria cópia para arquivo
        Message copy = new Message();
        copy.setChannel(message.getChannel());
        copy.setTo(ARCHIVE_EMAIL);
        copy.setSubject(COPY_PREFIX + message.getSubject());
        copy.setBody(message.getBody());
        copy.setStatus(MessageStatus.SENT);

        messages.add(copy);

        return messages;
    }
}
