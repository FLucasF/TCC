package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailHandler> handlers;

    public EmailSender(SentMessages sentMessages, boolean testMode, boolean archiveCopyEnabled) {
        this.sentMessages = sentMessages;
        this.handlers = buildHandlerChain(testMode, archiveCopyEnabled);
    }

    public Message send(String to, String subject, String body) {
        EmailContext context = new EmailContext(to, subject, body);

        for (EmailHandler handler : handlers) {
            if (!handler.handle(context)) {
                break;
            }
        }

        Message primaryMessage = sentMessages.add("EMAIL", context.to, context.subject, context.body, context.status);

        for (EmailContext.MessageRecord additional : context.additionalMessages()) {
            sentMessages.add("EMAIL", additional.to(), additional.subject(), additional.body(), additional.status());
        }

        return primaryMessage;
    }

    private static List<EmailHandler> buildHandlerChain(boolean testMode, boolean archiveCopyEnabled) {
        List<EmailHandler> chain = new ArrayList<>();
        chain.add(new EmailValidator());
        chain.add(new TestModeHandler(testMode));
        if (!testMode) {
            chain.add(new ArchiveCopyHandler(archiveCopyEnabled));
        }
        return chain;
    }
}
