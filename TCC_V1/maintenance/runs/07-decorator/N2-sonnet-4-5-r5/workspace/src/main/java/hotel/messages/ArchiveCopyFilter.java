package hotel.messages;

/** Generates a copy of every sent message for the accountant's archive. */
public final class ArchiveCopyFilter implements EmailFilter {

    private static final String ARCHIVE_ADDRESS = "arquivo@pousadamarealta.com.br";

    @Override
    public FilterResult apply(OutgoingMessage message) {
        String copySubject = "[Cópia] " + message.subject();
        OutgoingMessage copy = new OutgoingMessage(ARCHIVE_ADDRESS, copySubject, message.body());
        return FilterResult.allowWithExtra(message, copy);
    }
}
