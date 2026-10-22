package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageHandler> handlers;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
        this.handlers = new ArrayList<>();

        boolean testMode = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));
        handlers.add(new ValidationHandler());
        handlers.add(new TestModeHandler(testMode));
        handlers.add(new ArchiveCopyHandler(testMode));
    }

    public Message send(String to, String subject, String body) {
        MessageRequest request = new MessageRequest(to, subject, body);

        for (MessageHandler handler : handlers) {
            handler.handle(request);
        }

        Message mainMessage = sentMessages.add("EMAIL", request.to(), request.subject(), request.body(), request.status().name());

        for (Message generated : request.generatedMessages()) {
            sentMessages.add(generated.channel(), generated.to(), generated.subject(), generated.body(), generated.status());
        }

        return mainMessage;
    }
}
