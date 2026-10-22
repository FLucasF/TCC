package com.marealta.pousada.mail;

import java.util.List;

/**
 * As regras de e-mail desta instalação, na ordem em que valem. Para uma nova regra, basta criar a classe e
 * acrescentar uma linha aqui com o escopo desejado; outra instalação monta a sua própria lista.
 */
public final class MailRules {
    private MailRules() {
    }

    public static List<ScopedRule> marealta() {
        return List.of(
                new ScopedRule(new ValidAddressRule(), Scope.ALWAYS),
                new ScopedRule(new TestRedirectRule("testes@pousadamarealta.com.br"), Scope.TEST_ONLY),
                new ScopedRule(new ArchiveCopyRule("arquivo@pousadamarealta.com.br"), Scope.NON_TEST_ONLY));
    }
}
