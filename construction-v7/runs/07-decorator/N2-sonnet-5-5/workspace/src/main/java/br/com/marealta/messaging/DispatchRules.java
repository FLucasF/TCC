package br.com.marealta.messaging;

import java.util.List;

/**
 * Único lugar que lista as regras de envio, em ordem, e em que modo cada uma vale.
 * Para uma regra nova (horário, limite por hóspede...), crie um DispatchRule e acrescente uma linha aqui.
 */
public final class DispatchRules {
    private record Entry(RuleScope scope, DispatchRule rule) {}

    private DispatchRules() {}

    public static List<DispatchRule> forMode(boolean testMode) {
        List<Entry> entries = List.of(
                new Entry(RuleScope.ALWAYS, new InvalidAddressRule()),
                new Entry(RuleScope.TEST_ONLY, new TestRedirectRule()),
                new Entry(RuleScope.NON_TEST_ONLY, new ArchiveCopyRule()));
        return entries.stream()
                .filter(entry -> entry.scope().appliesTo(testMode))
                .map(Entry::rule)
                .toList();
    }
}
