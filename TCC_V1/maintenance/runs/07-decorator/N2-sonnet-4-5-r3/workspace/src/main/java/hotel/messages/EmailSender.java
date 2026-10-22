package hotel.messages;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final MessageSender sender;

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        MessageSender sender = new BasicMessageSender(sentMessages);
        if (testMode) {
            sender = new TestModeDecorator(sender);
        } else {
            sender = new ArchiveCopyDecorator(sender);
        }
        sender = new EmailValidationDecorator(sender, sentMessages);
        this.sender = sender;
    }

    public Message send(String to, String subject, String body) {
        return sender.send(to, subject, body);
    }
}
