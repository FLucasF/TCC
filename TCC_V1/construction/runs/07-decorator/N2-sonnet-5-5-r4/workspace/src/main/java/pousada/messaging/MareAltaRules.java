package pousada.messaging;

import java.util.List;

import pousada.messaging.MessagePipeline.Rule;

import static pousada.messaging.MessagePipeline.Applies.*;

/**
 * Regras de mensagem da Pousada Maré Alta, de fora para dentro. Outra instalação monta a sua lista.
 * Regras novas (horário, limite por hóspede) entram aqui como mais uma linha.
 */
public final class MareAltaRules {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MareAltaRules() {
    }

    public static List<Rule> rules(MessageSink recordStore) {
        return List.of(
                new Rule(OUTSIDE_TEST_ONLY, next -> new ArchiveCopy(next, ARCHIVE_ADDRESS)),
                new Rule(ALWAYS, next -> new BlockInvalidAddress(next, recordStore)),
                new Rule(TEST_ONLY, next -> new TestRedirect(next, TEST_ADDRESS)));
    }
}
