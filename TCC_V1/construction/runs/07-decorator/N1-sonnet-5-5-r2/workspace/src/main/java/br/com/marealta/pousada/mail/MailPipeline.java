package br.com.marealta.pousada.mail;

import java.util.ArrayList;
import java.util.List;

/** Passa cada mensagem pelas regras instaladas, na ordem, e devolve tudo o que deve ser registrado. */
public class MailPipeline {
    private final List<MessageRule> active;

    public MailPipeline(boolean testMode, List<InstalledRule> installed) {
        this.active = installed.stream()
                .filter(r -> r.scope().appliesTo(testMode))
                .map(InstalledRule::rule)
                .toList();
    }

    public List<Message> process(Message original) {
        List<Message> current = List.of(original);
        for (MessageRule rule : active) {
            List<Message> next = new ArrayList<>();
            for (Message message : current) {
                if (message.sent()) {
                    next.addAll(rule.apply(message));
                } else {
                    next.add(message);
                }
            }
            current = next;
        }
        return current;
    }
}
