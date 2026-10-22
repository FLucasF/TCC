package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Creates an archive copy of every sent message (not blocked ones) for the accountant. */
public final class ArchiveCopyRule implements MessageRule {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<OutgoingMessage> process(OutgoingMessage message) {
        if (message.isBlocked()) {
            return List.of(message);
        }
        OutgoingMessage archiveCopy = new OutgoingMessage(
                ARCHIVE_ADDRESS,
                "[Cópia] " + message.subject(),
                message.body());
        List<OutgoingMessage> result = new ArrayList<>();
        result.add(message);
        result.add(archiveCopy);
        return result;
    }
}
