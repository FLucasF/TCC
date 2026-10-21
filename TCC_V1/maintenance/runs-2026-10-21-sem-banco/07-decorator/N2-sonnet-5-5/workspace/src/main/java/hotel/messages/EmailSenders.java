package hotel.messages;

/** Decides, in one place, which rules wrap the real sending in each installation. */
public final class EmailSenders {

    private EmailSenders() {
    }

    public static EmailSender assemble(SentMessages sentMessages, boolean testMode, boolean archiveCopies) {
        EmailSender sender = new RecordingEmailSender(sentMessages);
        if (testMode) {
            sender = new TestModeEmailSender(sender);
        }
        sender = new ValidatingEmailSender(sender, sentMessages);
        if (archiveCopies && !testMode) {
            sender = new ArchivingEmailSender(sender);
        }
        return sender;
    }
}
