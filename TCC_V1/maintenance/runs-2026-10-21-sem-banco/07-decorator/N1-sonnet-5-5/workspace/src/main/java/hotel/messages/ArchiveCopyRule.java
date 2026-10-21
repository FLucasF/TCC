package hotel.messages;

import java.util.List;

/** Every message sent is followed by a copy for the archive. */
public final class ArchiveCopyRule implements MessageRule {

    private final String archiveAddress;

    public ArchiveCopyRule(String archiveAddress) {
        this.archiveAddress = archiveAddress;
    }

    @Override
    public List<Draft> apply(Draft draft) {
        return List.of(draft, draft.withTo(archiveAddress).withSubject("[Cópia] " + draft.subject()));
    }
}
