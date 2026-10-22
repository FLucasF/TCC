package br.com.marealta.mail;

import java.util.ArrayList;
import java.util.List;

/** Passa cada mensagem, na ordem, por todas as regras que valem no modo atual. */
public final class MailPipeline {
    private final List<MailRule> rules;

    public MailPipeline(boolean testMode, List<ScopedRule> configured) {
        this.rules = configured.stream()
                .filter(scoped -> scoped.scope().appliesTo(testMode))
                .map(ScopedRule::rule)
                .toList();
    }

    public List<Message> process(Message message) {
        List<Message> current = List.of(message);
        for (MailRule rule : rules) {
            List<Message> next = new ArrayList<>();
            for (Message m : current) {
                if (m.isSent()) {
                    next.addAll(rule.apply(m));
                } else {
                    next.add(m);
                }
            }
            current = next;
        }
        return current;
    }
}
