package hotel.messages;

public final class ArchiveCopyHandler implements MessageHandler {
    private final boolean testMode;

    public ArchiveCopyHandler(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void handle(MessageRequest request) {
        if (!testMode && request.status() != MessageStatus.BLOCKED) {
            String archiveSubject = "[Cópia] " + request.subject();
            request.addGeneratedMessage(new Message(
                    0,
                    "EMAIL",
                    "arquivo@pousadamarealta.com.br",
                    archiveSubject,
                    request.body(),
                    MessageStatus.SENT.name()));
        }
    }
}
