package pousada.messaging;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Monta a cadeia de regras em volta do destino final. Cada regra diz em que modo vale e como
 * embrulha o próximo; a combinação de cada instalação é uma lista de regras, na ordem de fora para dentro.
 */
public final class MessagePipeline {

    public enum Applies {
        ALWAYS, TEST_ONLY, OUTSIDE_TEST_ONLY;

        boolean in(boolean testMode) {
            return switch (this) {
                case ALWAYS -> true;
                case TEST_ONLY -> testMode;
                case OUTSIDE_TEST_ONLY -> !testMode;
            };
        }
    }

    public record Rule(Applies applies, UnaryOperator<MessageSink> wrap) {
    }

    private MessagePipeline() {
    }

    public static MessageSink assemble(boolean testMode, MessageSink destination, List<Rule> outerToInner) {
        MessageSink sink = destination;
        for (int i = outerToInner.size() - 1; i >= 0; i--) {
            Rule rule = outerToInner.get(i);
            if (rule.applies().in(testMode)) {
                sink = rule.wrap().apply(sink);
            }
        }
        return sink;
    }
}
