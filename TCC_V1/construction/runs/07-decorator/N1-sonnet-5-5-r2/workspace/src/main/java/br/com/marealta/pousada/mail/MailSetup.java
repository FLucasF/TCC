package br.com.marealta.pousada.mail;

import br.com.marealta.pousada.mail.rules.ArchiveCopyRule;
import br.com.marealta.pousada.mail.rules.TestRedirectRule;
import br.com.marealta.pousada.mail.rules.ValidAddressRule;
import java.util.List;

/**
 * As regras de e-mail desta instalação, na ordem em que valem. Uma regra nova (ou uma instalação sem alguma
 * delas) é só uma linha aqui. Regras que bloqueiam vêm antes das que copiam, e o desvio para teste vem por
 * último, para que nada que tenha sido gerado saia para endereço real.
 */
public final class MailSetup {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private MailSetup() {
    }

    public static List<InstalledRule> pousadaMareAlta() {
        return List.of(
                new InstalledRule(Scope.ALWAYS, new ValidAddressRule()),
                new InstalledRule(Scope.LIVE_ONLY, new ArchiveCopyRule(ARCHIVE_ADDRESS)),
                new InstalledRule(Scope.TEST_ONLY, new TestRedirectRule(TEST_ADDRESS)));
    }
}
