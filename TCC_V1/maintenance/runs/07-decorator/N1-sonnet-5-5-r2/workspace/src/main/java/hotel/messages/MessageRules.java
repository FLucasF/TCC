package hotel.messages;

import java.util.List;

/** The rules of an installation of the system: which ones, and when each is in force. */
public final class MessageRules {

    private MessageRules() {
    }

    public static List<MessageRule> forInstallation(boolean testMode) {
        return List.of(
                scoped(Scope.ALWAYS, testMode, new ValidAddress()),
                scoped(Scope.TEST_MODE_ONLY, testMode, new TestRedirect()),
                scoped(Scope.OUTSIDE_TEST_MODE_ONLY, testMode, new ArchiveCopy()));
    }

    private static MessageRule scoped(Scope scope, boolean testMode, MessageRule rule) {
        return new ScopedRule(scope, testMode, rule);
    }
}
