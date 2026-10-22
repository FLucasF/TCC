package br.com.marealta.pousada.messaging;

import java.util.List;

/** Passa cada mensagem, na ordem, pelas regras que valem para o modo atual. */
public final class MessagePipeline {
    private final List<MessagePolicy> policies;

    public MessagePipeline(List<Rule> rules, boolean testMode) {
        this.policies = rules.stream()
                .filter(rule -> rule.scope().appliesTo(testMode))
                .map(Rule::policy)
                .toList();
    }

    public List<Message> process(Message draft) {
        List<Message> current = List.of(draft);
        for (MessagePolicy policy : policies) {
            current = current.stream().flatMap(message -> policy.apply(message).stream()).toList();
        }
        return current;
    }
}
