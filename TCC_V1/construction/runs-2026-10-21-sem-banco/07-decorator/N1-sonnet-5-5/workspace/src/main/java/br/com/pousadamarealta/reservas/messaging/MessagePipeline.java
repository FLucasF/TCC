package br.com.pousadamarealta.reservas.messaging;

import java.util.List;

/** Passa a mensagem, na ordem, pelas regras que valem no modo atual. Mensagem bloqueada não segue adiante. */
public final class MessagePipeline {

    private final List<MessageRule> rules;

    public MessagePipeline(boolean testMode, List<ScopedRule> scopedRules) {
        this.rules = scopedRules.stream()
                .filter(scoped -> scoped.scope().appliesTo(testMode))
                .map(ScopedRule::rule)
                .toList();
    }

    public void run(Draft draft) {
        for (MessageRule rule : rules) {
            if (draft.isBlocked()) {
                return;
            }
            rule.apply(draft);
        }
    }
}
