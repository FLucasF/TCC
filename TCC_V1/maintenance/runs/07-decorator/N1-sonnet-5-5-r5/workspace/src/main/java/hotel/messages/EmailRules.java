package hotel.messages;

import java.util.List;

/** The rules of each installation of the system. */
public final class EmailRules {

    private EmailRules() {
    }

    /** Pousada Maré Alta. The order matters: an invalid address is blocked before anything else touches it. */
    public static List<ScopedRule> mareAlta() {
        return List.of(
                new ScopedRule(RuleScope.ALWAYS, new BlockInvalidAddress()),
                new ScopedRule(RuleScope.TEST_MODE_ONLY, new RedirectToTests()),
                new ScopedRule(RuleScope.NORMAL_MODE_ONLY, new CopyToArchive()));
    }
}
