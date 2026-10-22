package br.com.marealta.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Regras que existem e como cada instalação as combina. A configuração é uma lista "regra[:escopo]" separada
 * por vírgulas, na ordem de execução; escopo é always, test ou live. Regra nova entra aqui, numa linha só.
 */
public final class RuleCatalog {

    public static final String DEFAULT_SPEC = "address-validation,test-redirect:test,archive-copy:live";

    private static final Map<String, Supplier<MessageRule>> AVAILABLE = Map.of(
            "address-validation", AddressValidationRule::new,
            "test-redirect", () -> new TestRedirectRule("testes@pousadamarealta.com.br"),
            "archive-copy", () -> new ArchiveCopyRule("arquivo@pousadamarealta.com.br"));

    private static final Map<String, RuleScope> SCOPES = Map.of(
            "always", RuleScope.ALWAYS,
            "test", RuleScope.TEST_ONLY,
            "live", RuleScope.LIVE_ONLY);

    private RuleCatalog() {
    }

    public static List<ConfiguredRule> parse(String spec) {
        String effective = spec == null || spec.isBlank() ? DEFAULT_SPEC : spec;
        List<ConfiguredRule> rules = new ArrayList<>();
        for (String item : effective.split(",")) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.trim().split(":", 2);
            Supplier<MessageRule> rule = AVAILABLE.get(parts[0].trim());
            if (rule == null) {
                throw new IllegalArgumentException("Regra de mensagem desconhecida: " + parts[0]);
            }
            RuleScope scope = parts.length == 1 ? RuleScope.ALWAYS : SCOPES.get(parts[1].trim().toLowerCase(Locale.ROOT));
            if (scope == null) {
                throw new IllegalArgumentException("Escopo desconhecido: " + parts[1]);
            }
            rules.add(new ConfiguredRule(rule.get(), scope));
        }
        return rules;
    }
}
