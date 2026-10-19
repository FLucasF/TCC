package pousada.messaging;

import java.util.ArrayList;
import java.util.List;

/**
 * Aplica as regras, em ordem, às mensagens. Só valem as regras do modo atual, e mensagem bloqueada
 * não passa pelas regras seguintes. Uma regra nova entra na lista de {@link #standard}.
 */
public class MessagePipeline {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private final List<MessageRule> rules;
    private final boolean testMode;

    public MessagePipeline(List<MessageRule> rules, boolean testMode) {
        this.rules = rules;
        this.testMode = testMode;
    }

    public static MessagePipeline standard(boolean testMode) {
        return new MessagePipeline(List.of(
                new InvalidAddressRule(),
                new TestRedirectRule(TEST_ADDRESS),
                new ArchiveCopyRule(ARCHIVE_ADDRESS)), testMode);
    }

    public List<Message> process(Message message) {
        List<Message> current = List.of(message);
        for (MessageRule rule : rules) {
            if (!rule.scope().appliesIn(testMode)) {
                continue;
            }
            List<Message> next = new ArrayList<>();
            for (Message m : current) {
                if (m.status() == MessageStatus.SENT) {
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
