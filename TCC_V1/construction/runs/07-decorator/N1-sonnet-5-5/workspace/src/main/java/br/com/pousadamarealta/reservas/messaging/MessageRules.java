package br.com.pousadamarealta.reservas.messaging;

import static br.com.pousadamarealta.reservas.messaging.ScopedRule.always;
import static br.com.pousadamarealta.reservas.messaging.ScopedRule.liveOnly;
import static br.com.pousadamarealta.reservas.messaging.ScopedRule.testOnly;

import java.util.List;

/**
 * A combinação de regras de e-mail desta instalação, em ordem. É o único lugar a mexer para
 * ligar, desligar ou acrescentar uma regra, e para escolher se vale sempre, só em teste ou só fora dele.
 * <p>
 * Já existem, mas não estão ligadas (faltam os valores combinados com a pousada):
 * {@code always(new QuietHoursRule(clock, LocalTime.of(22, 0), LocalTime.of(8, 0)))} e
 * {@code always(new DailyLimitRule(clock, 5))}; entram antes do redirecionamento de teste.
 */
public final class MessageRules {

    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private MessageRules() {
    }

    public static List<ScopedRule> marealta() {
        return List.of(
                always(new ValidAddressRule()),
                testOnly(new TestRedirectRule(TEST_ADDRESS)),
                liveOnly(new ArchiveCopyRule(ARCHIVE_ADDRESS)));
    }
}
