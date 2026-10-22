package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Creates a copy of each message to the archive. */
public final class ArchiveCopyHandler implements MessageHandler {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";

    @Override
    public List<MessageToStore> handle(MessageToStore message) {
        List<MessageToStore> messages = new ArrayList<>();
        messages.add(message);
        if ("SENT".equals(message.status())) {
            messages.add(new MessageToStore(ARCHIVE_EMAIL, "[Cópia] " + message.subject(), message.body(), "SENT"));
        }
        return messages;
    }
}
