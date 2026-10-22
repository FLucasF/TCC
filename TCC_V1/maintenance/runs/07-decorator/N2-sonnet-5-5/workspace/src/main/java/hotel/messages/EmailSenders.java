package hotel.messages;

/** Where the e-mail rules of an installation are put together: the only place that says which ones apply. */
public final class EmailSenders {

    private EmailSenders() {
    }

    public static EmailSender create(SentMessages sentMessages, MessagingSettings settings) {
        EmailSender sender = new RecordingEmailSender(sentMessages);
        if (settings.testMode()) {
            sender = new TestModeEmailSender(sender);
        }
        sender = new ValidatingEmailSender(sender, sentMessages);
        if (!settings.testMode() && settings.archiveCopy()) {
            sender = new ArchivingEmailSender(sender);
        }
        return sender;
    }
}
