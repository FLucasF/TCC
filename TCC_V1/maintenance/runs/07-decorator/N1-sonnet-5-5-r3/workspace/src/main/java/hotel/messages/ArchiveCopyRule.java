package hotel.messages;

import java.util.List;

/** Every message sent is followed by a copy to the archive, for the accountant. */
public final class ArchiveCopyRule implements MessageRule {

    static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        OutgoingMessage copy = message.withTo(ARCHIVE_ADDRESS).withSubject("[Cópia] " + message.subject());
        return List.of(message, copy);
    }
}
