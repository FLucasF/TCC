package com.marealta.mail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * The one place that decides which rules wrap the outbox for this installation.
 * Rules are listed from the outermost to the innermost; a new rule is one class and one line here.
 */
public final class MailPipeline {
    private enum Scope {
        ALWAYS, TEST_ONLY, LIVE_ONLY;

        boolean applies(boolean testMode) {
            return this == ALWAYS || (this == TEST_ONLY) == testMode;
        }
    }

    private record Rule(Scope scope, UnaryOperator<Outbox> wrap) {
    }

    private MailPipeline() {
    }

    public static Outbox build(Outbox log, MailSettings settings) {
        List<Rule> rules = new ArrayList<>();
        rules.add(new Rule(Scope.ALWAYS, Serialized::new));
        rules.add(new Rule(Scope.ALWAYS, AddressValidation::new));
        if (settings.archiveCopy()) {
            rules.add(new Rule(Scope.LIVE_ONLY, next -> new ArchiveCopy(next, MailSettings.ARCHIVE_ADDRESS)));
        }
        rules.add(new Rule(Scope.TEST_ONLY, next -> new TestRedirect(next, MailSettings.TEST_ADDRESS)));

        Outbox outbox = log;
        for (int i = rules.size() - 1; i >= 0; i--) {
            Rule rule = rules.get(i);
            if (rule.scope().applies(settings.testMode())) {
                outbox = rule.wrap().apply(outbox);
            }
        }
        return outbox;
    }
}
