package hotel.messages;

/** Creates an archive copy of sent messages. */
final class ArchiveCopyRule implements EmailRule {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private final boolean testMode;

    ArchiveCopyRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(OutboundMessage message) {
        if (testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }
        OutboundMessage copy = new OutboundMessage(
                ARCHIVE_ADDRESS,
                "[Cópia] " + message.subject(),
                message.body()
        );
        message.addCopy(copy);
    }
}
