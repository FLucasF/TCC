package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Creates an archive copy of every sent message (not blocked). Only in production mode. */
public final class ArchiveCopy implements SendingRule {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    private final boolean testMode;

    public ArchiveCopy(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (testMode || message.status() == MessageStatus.BLOCKED) {
            return List.of(message);
        }
        List<OutgoingMessage> result = new ArrayList<>();
        result.add(message);
        result.add(new OutgoingMessage(
                ARCHIVE_EMAIL,
                "[Cópia] " + message.subject(),
                message.body()));
        return result;
    }
}
