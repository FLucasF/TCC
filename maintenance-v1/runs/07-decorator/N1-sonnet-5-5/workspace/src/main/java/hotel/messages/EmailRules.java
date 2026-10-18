package hotel.messages;

import java.util.List;

/**
 * The rules the inn applies to its e-mails, in order, and when each one is in force. To add a rule, write
 * it and list it here with its scope.
 */
final class EmailRules {

    /** When a rule is in force. */
    enum Scope {
        ALWAYS,
        TEST_MODE_ONLY,
        NORMAL_MODE_ONLY;

        boolean covers(boolean testMode) {
            return switch (this) {
                case ALWAYS -> true;
                case TEST_MODE_ONLY -> testMode;
                case NORMAL_MODE_ONLY -> !testMode;
            };
        }
    }

    private record Entry(Scope scope, EmailRule rule) {
    }

    private EmailRules() {
    }

    static List<EmailRule> forMode(boolean testMode) {
        return List.of(
                        new Entry(Scope.ALWAYS, new ValidAddressRule()),
                        new Entry(Scope.TEST_MODE_ONLY, new TestRedirectRule()),
                        new Entry(Scope.NORMAL_MODE_ONLY, new ArchiveCopyRule()))
                .stream()
                .filter(entry -> entry.scope().covers(testMode))
                .map(Entry::rule)
                .toList();
    }
}
