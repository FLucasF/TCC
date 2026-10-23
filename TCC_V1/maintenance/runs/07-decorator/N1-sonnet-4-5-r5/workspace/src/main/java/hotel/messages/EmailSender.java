package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageProcessor> processors;

    public EmailSender(SentMessages sentMessages, List<MessageProcessor> processors) {
        this.sentMessages = sentMessages;
        this.processors = new ArrayList<>(processors);
    }

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, List.of());
    }

    public Message send(String to, String subject, String body) {
        OutgoingMessage message = new OutgoingMessage("EMAIL", to, subject, body);
        for (MessageProcessor processor : processors) {
            processor.process(message);
        }
        return sentMessages.add(message);
    }
}
