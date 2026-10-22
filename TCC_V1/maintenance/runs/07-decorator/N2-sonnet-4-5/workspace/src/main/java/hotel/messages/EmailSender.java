package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final MessageProcessor processor;

    public EmailSender(SentMessages sentMessages, MessageProcessor processor) {
        this.sentMessages = sentMessages;
        this.processor = processor;
    }

    public Message send(String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(to, subject, body);
        List<ProcessedMessage.MessageToStore> processed = processor.process(outgoing);

        Message firstMessage = null;
        for (ProcessedMessage.MessageToStore msg : processed) {
            Message stored = sentMessages.add("EMAIL", msg.to(), msg.subject(), msg.body(), msg.status());
            if (firstMessage == null) {
                firstMessage = stored;
            }
        }

        return firstMessage;
    }
}
