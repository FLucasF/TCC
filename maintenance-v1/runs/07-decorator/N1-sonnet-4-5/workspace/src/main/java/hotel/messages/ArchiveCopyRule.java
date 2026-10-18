package hotel.messages;

/** Creates an archive copy of every sent message, but only in production mode. */
final class ArchiveCopyRule implements MessageRule {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";
    private static final String SUBJECT_PREFIX = "[Cópia] ";

    @Override
    public MessageToSend apply(MessageToSend message, boolean testMode) {
        if (testMode || message.status() == MessageStatus.BLOCKED) {
            return message;
        }

        MessageToSend archiveCopy = new MessageToSend(
                ARCHIVE_ADDRESS,
                SUBJECT_PREFIX + message.subject(),
                message.body(),
                MessageStatus.SENT);
        message.addAdditionalMessage(archiveCopy);
        return message;
    }
}
