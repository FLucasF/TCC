package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Creates a copy of sent messages for the archive. */
public final class ArchiveCopyHandler implements MessageHandler {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private final boolean testMode;

    public ArchiveCopyHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public ProcessedMessage handle(OutgoingMessage message) {
        if (testMode) {
            return ProcessedMessage.sent(message.to(), message.subject(), message.body());
        }

        List<ProcessedMessage.MessageToStore> messages = new ArrayList<>();
        messages.add(new ProcessedMessage.MessageToStore(
            message.to(), message.subject(), message.body(), MessageStatus.SENT
        ));
        messages.add(new ProcessedMessage.MessageToStore(
            ARCHIVE_ADDRESS, "[Cópia] " + message.subject(), message.body(), MessageStatus.SENT
        ));

        return ProcessedMessage.multiple(messages);
    }
}
