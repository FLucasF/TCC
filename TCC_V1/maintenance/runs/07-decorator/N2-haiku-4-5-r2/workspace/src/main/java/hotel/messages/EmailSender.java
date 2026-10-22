package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageHandler> handlers;

    public EmailSender(SentMessages sentMessages, List<MessageHandler> handlers) {
        this.sentMessages = sentMessages;
        this.handlers = handlers;
    }

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, List.of(new SendAsIsHandler()));
    }

    public void send(String to, String subject, String body) {
        var messages = List.of(new MessageToStore(to, subject, body, "SENT"));
        for (MessageHandler handler : handlers) {
            messages = messages.stream()
                    .flatMap(msg -> handler.handle(msg).stream())
                    .toList();
            if (messages.isEmpty()) break;
        }
        sentMessages.addAll(messages);
    }

    /** Simple handler that sends messages as-is with SENT status. Used for tests. */
    private static final class SendAsIsHandler implements MessageHandler {
        @Override
        public List<MessageToStore> handle(MessageToStore message) {
            return List.of(message);
        }
    }
}
