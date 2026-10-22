package br.com.marealta.messaging;

import java.util.List;

public final class MessagePipeline {
    private MessagePipeline() {}

    /**
     * Monta o envio: parte do envio real e embrulha com cada regra que vale no modo atual,
     * na ordem da lista (a primeira fica mais por dentro, a última é a primeira a ver a mensagem).
     */
    public static MessageSender assemble(MessageSender base, List<SendingRule> rules, boolean testMode) {
        MessageSender sender = base;
        for (SendingRule rule : rules) {
            if (rule.scope().appliesTo(testMode)) {
                sender = rule.wrapper().apply(sender);
            }
        }
        return sender;
    }
}
