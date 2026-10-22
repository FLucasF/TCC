package br.com.marealta.messaging;

import java.util.List;

/**
 * As regras de envio desta instalação. Outra pousada monta a sua lista aqui:
 * tirar uma regra é apagar a linha; uma regra nova (horário, limite por hóspede) é um decorador novo e uma linha.
 */
public final class Installation {
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private Installation() {}

    public static MessageSender marealta(MessageStore store, boolean testMode) {
        List<SendingRule> rules = List.of(
                new SendingRule(Scope.TEST_MODE_ONLY, next -> new TestModeSender(TEST_ADDRESS, next)),
                new SendingRule(Scope.ALWAYS, next -> new ValidatingSender(store, next)),
                new SendingRule(Scope.OUTSIDE_TEST_MODE_ONLY, next -> new ArchiveCopySender(ARCHIVE_ADDRESS, next)));
        return MessagePipeline.assemble(new OutboxSender(store), rules, testMode);
    }
}
