package br.com.marealta.messaging;

import java.util.List;

/** Passa cada mensagem pela cadeia de regras e guarda o resultado na lista de mensagens. */
public class Notifier {
    private final DispatchRule.Next chain;
    private final MessageLog log;

    public Notifier(List<DispatchRule> rules, MessageLog log) {
        this.log = log;
        DispatchRule.Next next = message -> List.of(message);
        for (int i = rules.size() - 1; i >= 0; i--) {
            DispatchRule rule = rules.get(i);
            DispatchRule.Next following = next;
            next = message -> rule.apply(message, following);
        }
        this.chain = next;
    }

    public void send(Channel channel, String to, String subject, String body) {
        log.addAll(chain.send(Message.draft(channel, to, subject, body)));
    }
}
