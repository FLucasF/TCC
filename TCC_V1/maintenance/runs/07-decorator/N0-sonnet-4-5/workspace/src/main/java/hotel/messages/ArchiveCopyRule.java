package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class ArchiveCopyRule implements MessageRule {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String COPY_PREFIX = "[Cópia] ";

    private final boolean isTestMode;

    public ArchiveCopyRule(boolean isTestMode) {
        this.isTestMode = isTestMode;
    }

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (isTestMode || message.status() == MessageStatus.BLOCKED) {
            return List.of(message);
        }
        List<OutgoingMessage> result = new ArrayList<>();
        result.add(message);
        OutgoingMessage copy = new OutgoingMessage(ARCHIVE_EMAIL, COPY_PREFIX + message.subject(), message.body());
        result.add(copy);
        return result;
    }
}
