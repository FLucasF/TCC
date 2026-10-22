package br.com.marealta.message;

import java.util.List;

/** A sequência de passos é uma só; cada instalação escolhe quais regras entram e em que modo. */
public class MessagePipeline {

    private final List<ConfiguredRule> rules;
    private final boolean testMode;

    public MessagePipeline(List<ConfiguredRule> rules, boolean testMode) {
        this.rules = List.copyOf(rules);
        this.testMode = testMode;
    }

    public List<Message> process(Message draft) {
        Delivery delivery = new Delivery(draft);
        for (ConfiguredRule configured : rules) {
            if (delivery.isBlocked()) {
                break;
            }
            if (configured.scope().appliesTo(testMode)) {
                configured.rule().apply(delivery);
            }
        }
        return delivery.result();
    }
}
