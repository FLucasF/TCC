package br.com.marealta.pousada.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * As regras que existem e a combinação usada por cada instalação.
 * Uma regra nova entra com uma classe e uma linha em {@link #AVAILABLE}.
 */
public final class MessageRules {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private static final Map<String, Supplier<MessagePolicy>> AVAILABLE = Map.of(
            "valid-address", ValidAddressPolicy::new,
            "test-redirect", () -> new TestRedirectPolicy(TEST_ADDRESS),
            "archive-copy", () -> new ArchiveCopyPolicy(ARCHIVE_ADDRESS));

    /** A ordem importa: valida antes de redirecionar, e copia por último. */
    public static final String DEFAULT = "valid-address,test-redirect:test,archive-copy:live";

    private MessageRules() {
    }

    /** Lê uma lista como "valid-address,test-redirect:test,archive-copy:live" (escopos: always, test, live). */
    public static List<Rule> parse(String spec) {
        List<Rule> rules = new ArrayList<>();
        for (String item : spec.split(",")) {
            String[] parts = item.trim().split(":");
            Supplier<MessagePolicy> policy = AVAILABLE.get(parts[0]);
            if (policy == null) {
                throw new IllegalArgumentException("Regra de mensagem desconhecida: " + parts[0]);
            }
            rules.add(new Rule(policy.get(), parts.length > 1 ? scopeOf(parts[1]) : Scope.ALWAYS));
        }
        return rules;
    }

    private static Scope scopeOf(String name) {
        return switch (name) {
            case "always" -> Scope.ALWAYS;
            case "test" -> Scope.TEST_ONLY;
            case "live" -> Scope.LIVE_ONLY;
            default -> throw new IllegalArgumentException("Escopo de regra desconhecido: " + name);
        };
    }
}
