package hotel.messages;

/**
 * Sends a copy of every message to the archive address, so the accountant has a record of everything
 * the inn sent. The copy has the same text and the subject starts with "[Cópia] ". Blocked messages
 * do not generate a copy.
 */
final class ArchiveCopyHandler implements EmailHandler {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private final boolean enabled;

    ArchiveCopyHandler(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean handle(EmailContext context) {
        if (enabled && context.status == MessageStatus.SENT) {
            context.addMessage(ARCHIVE_ADDRESS, "[Cópia] " + context.subject, context.body, MessageStatus.SENT);
        }
        return true;
    }
}
