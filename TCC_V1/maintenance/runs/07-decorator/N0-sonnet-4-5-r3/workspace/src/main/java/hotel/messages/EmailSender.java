package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final MessageProcessor messageProcessor;

    public EmailSender(SentMessages sentMessages, MessageProcessor messageProcessor) {
        this.sentMessages = sentMessages;
        this.messageProcessor = messageProcessor;
    }

    public Message send(String to, String subject, String body) {
        List<ProcessedMessage> processedMessages = messageProcessor.process(to, subject, body);
        Message firstMessage = null;
        for (ProcessedMessage processed : processedMessages) {
            Message message = sentMessages.add("EMAIL", processed.to(), processed.subject(), processed.body(), processed.status());
            if (firstMessage == null) {
                firstMessage = message;
            }
        }
        return firstMessage;
    }
}
