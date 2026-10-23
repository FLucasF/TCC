package hotel.messages;

/** Cria cópia de mensagens enviadas para o arquivo. */
public final class ArchiveCopier implements MessageProcessor {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private final boolean testMode;

    public ArchiveCopier(boolean testMode) {
        this.testMode = testMode;
    }

    @Override
    public void process(OutgoingMessage message) {
        if (testMode || message.status() == MessageStatus.BLOCKED) {
            return;
        }
        OutgoingMessage copy = new OutgoingMessage(
                message.channel(),
                ARCHIVE_EMAIL,
                "[Cópia] " + message.subject(),
                message.body());
        message.addMessage(copy);
    }
}
