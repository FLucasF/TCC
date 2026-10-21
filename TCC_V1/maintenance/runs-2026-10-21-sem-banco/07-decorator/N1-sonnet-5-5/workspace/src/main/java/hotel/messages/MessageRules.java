package hotel.messages;

import java.util.List;

/**
 * The rules of an installation of the system, in the order they are applied. To change what an
 * installation does with its e-mails, change this list.
 */
public final class MessageRules {

    private static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";
    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    private MessageRules() {
    }

    public static List<ScopedRule> pousadaMareAlta() {
        return List.of(
                ScopedRule.always(new ValidAddressRule()),
                ScopedRule.onlyInTestMode(new TestRedirectRule(TEST_ADDRESS)),
                ScopedRule.onlyOutsideTestMode(new ArchiveCopyRule(ARCHIVE_ADDRESS)));
    }
}
