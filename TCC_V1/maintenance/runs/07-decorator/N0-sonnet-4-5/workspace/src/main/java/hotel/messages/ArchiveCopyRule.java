package hotel.messages;

final class ArchiveCopyRule implements MessageRule {

    private final boolean testMode;

    ArchiveCopyRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(ProcessedMessage message) {
        if (testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }

        ProcessedMessage copy = new ProcessedMessage(
            "arquivo@pousadamarealta.com.br",
            "[Cópia] " + message.subject(),
            message.body()
        );
        message.addDerived(copy);
    }
}
