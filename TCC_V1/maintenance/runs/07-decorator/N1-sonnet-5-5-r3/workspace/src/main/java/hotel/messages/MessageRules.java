package hotel.messages;

import java.util.List;

/**
 * The rules this installation of the system applies to its e-mails, in the order they run. Another installation
 * (or a new rule) changes this list and nothing else.
 */
public final class MessageRules {

    private MessageRules() {
    }

    public static List<ScopedRule> ofTheInn() {
        return List.of(
                new ScopedRule(RuleScope.ALWAYS, new ValidAddressRule()),
                new ScopedRule(RuleScope.OUTSIDE_TEST_MODE_ONLY, new ArchiveCopyRule()),
                new ScopedRule(RuleScope.TEST_MODE_ONLY, new TestRedirectRule()));
    }
}
