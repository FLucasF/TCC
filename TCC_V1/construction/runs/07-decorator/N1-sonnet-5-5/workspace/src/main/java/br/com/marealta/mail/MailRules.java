package br.com.marealta.mail;

import java.util.List;

/**
 * As regras de e-mail desta instalação, na ordem em que valem. Acrescentar uma regra nova, ou tirar uma, é
 * mexer só aqui (mais a classe da regra). A ordem importa: a cópia para o arquivo vem por último, para só
 * copiar o que de fato vai sair.
 */
public final class MailRules {
    private MailRules() {
    }

    public static List<ScopedRule> forThisInstallation() {
        return List.of(
                new ScopedRule(Scope.ALWAYS, new InvalidAddressRule()),
                new ScopedRule(Scope.TEST_ONLY, new TestRedirectRule("testes@pousadamarealta.com.br")),
                new ScopedRule(Scope.OUTSIDE_TEST_ONLY, new ArchiveCopyRule("arquivo@pousadamarealta.com.br")));
    }
}
