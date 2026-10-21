package br.com.marealta;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Monta a cadeia de regras de envio de uma instalação. Para uma nova regra, basta uma classe
 * decoradora e uma linha em RULES; para outra pousada, basta mudar a lista.
 */
public final class MessagingSetup {
    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private enum Scope {
        ALWAYS, TEST_ONLY, OUTSIDE_TEST;

        boolean appliesTo(boolean testMode) {
            return this == ALWAYS || (this == TEST_ONLY) == testMode;
        }
    }

    private record Rule(Scope scope, UnaryOperator<MessageSender> wrap) {}

    private MessagingSetup() {}

    /** Constrói o remetente; a primeira regra aplicável é a mais externa. */
    public static MessageSender build(Outbox outbox, boolean testMode) {
        List<Rule> rules = List.of(
                new Rule(Scope.ALWAYS, next -> new BlockInvalidAddress(next, outbox)),
                new Rule(Scope.OUTSIDE_TEST, next -> new ArchiveCopy(next, ARCHIVE_ADDRESS)),
                new Rule(Scope.TEST_ONLY, next -> new RedirectToTestAddress(next, TEST_ADDRESS)));

        MessageSender sender = outbox;
        for (Rule rule : rules.reversed()) {
            if (rule.scope().appliesTo(testMode)) {
                sender = rule.wrap().apply(sender);
            }
        }
        return sender;
    }
}
