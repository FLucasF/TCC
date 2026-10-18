package hotel.messages;

import java.util.Collections;
import java.util.List;

/**
 * Generates an archive copy for every sent email. Blocked messages and test mode messages don't
 * generate copies.
 */
public final class ArchiveCopyRule implements EmailRule {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[Cópia] ";

    private final boolean testMode;

    public ArchiveCopyRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<OutgoingEmail> process(OutgoingEmail email) {
        if (testMode || email.status() == MessageStatus.BLOCKED) {
            return Collections.emptyList();
        }

        OutgoingEmail copy = new OutgoingEmail(
                ARCHIVE_ADDRESS,
                SUBJECT_PREFIX + email.subject(),
                email.body()
        );

        return List.of(copy);
    }
}
