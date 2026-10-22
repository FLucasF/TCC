package hotel.messages;

public final class ArchiveCopyRule implements EmailRule {

    private final boolean testMode;

    public ArchiveCopyRule(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void apply(ProcessingResult result) {
        if (testMode) {
            return;
        }
        OutgoingMessage message = result.original();
        if (message.status() == MessageStatus.BLOCKED) {
            return;
        }
        String copySubject = "[Cópia] " + message.subject();
        OutgoingMessage copy = new OutgoingMessage(
                "arquivo@pousadamarealta.com.br",
                copySubject,
                message.body(),
                MessageStatus.SENT);
        result.addMessage(copy);
    }
}
