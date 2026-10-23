package com.pousada.marealta.messaging;

import com.pousada.marealta.domain.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline de processamento de mensagens que aplica múltiplas regras em sequência.
 * Cada regra pode modificar, bloquear ou criar cópias de mensagens.
 */
public class MessagePipeline {
    private final List<MessageRule> rules;

    public MessagePipeline(List<MessageRule> rules) {
        this.rules = rules;
    }

    /**
     * Processa uma mensagem através de todas as regras configuradas.
     *
     * @param message Mensagem original
     * @return Lista de todas as mensagens geradas (original modificada + cópias)
     */
    public List<Message> process(Message message) {
        List<Message> messages = new ArrayList<>();
        messages.add(message);

        for (MessageRule rule : rules) {
            List<Message> processedMessages = new ArrayList<>();
            for (Message msg : messages) {
                processedMessages.addAll(rule.process(msg));
            }
            messages = processedMessages;
        }

        return messages;
    }
}
