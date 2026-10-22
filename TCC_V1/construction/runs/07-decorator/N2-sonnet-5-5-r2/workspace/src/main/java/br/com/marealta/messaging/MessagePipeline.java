package br.com.marealta.messaging;

import java.util.List;

/** Monta o envio: as regras que valem no modo atual embrulham o envio final, a primeira da lista por fora. */
public final class MessagePipeline {
    private MessagePipeline() {}

    public static MessageSender build(boolean testMode, MessageSender last, List<MessageRule> rules) {
        MessageSender sender = last;
        for (int i = rules.size() - 1; i >= 0; i--) {
            MessageRule rule = rules.get(i);
            if (rule.scope().appliesTo(testMode)) {
                sender = rule.wrapper().apply(sender);
            }
        }
        return sender;
    }
}
