package br.com.marealta.messaging;

import java.util.List;

/**
 * The message rules of each installation. Another installation gets its own list here:
 * a different combination of the same rules (or new ones) and the scope of each.
 */
public final class Installation {
    public static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    public static final String TEST_ADDRESS = "testes@pousadamarealta.com.br";

    private Installation() {
    }

    public static List<ScopedRule> mareAlta() {
        return List.of(
                ScopedRule.always(AddressValidation::new),
                ScopedRule.liveOnly((next, blocked) -> new ArchiveCopy(ARCHIVE_ADDRESS, next)),
                ScopedRule.testOnly((next, blocked) -> new TestRedirect(TEST_ADDRESS, next)));
    }
}
