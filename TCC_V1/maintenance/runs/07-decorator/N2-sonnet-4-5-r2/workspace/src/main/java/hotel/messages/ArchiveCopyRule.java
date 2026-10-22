package hotel.messages;

/** Creates an archive copy of every sent message, unless in test mode. */
final class ArchiveCopyRule implements MessageRule {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private final boolean testMode;

    ArchiveCopyRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(MessageContext context) {
        if (context.isBlocked() || testMode) {
            return;
        }
        OutgoingMessage original = context.message();
        String copySubject = "[Cópia] " + original.subject();
        context.addAdditionalMessage(new OutgoingMessage(ARCHIVE_ADDRESS, copySubject, original.body()));
    }
}
