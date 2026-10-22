package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final EmailProcessor processor;

    public EmailSender(SentMessages sentMessages, EmailProcessor processor) {
        this.sentMessages = sentMessages;
        this.processor = processor;
    }

    public List<Message> send(String to, String subject, String body) {
        ProcessingResult result = processor.process(to, subject, body);
        List<Message> saved = new ArrayList<>();
        saved.add(sentMessages.add("EMAIL", result.original().to(), result.original().subject(),
                result.original().body(), result.original().status()));
        for (OutgoingMessage additional : result.additional()) {
            saved.add(sentMessages.add("EMAIL", additional.to(), additional.subject(),
                    additional.body(), additional.status()));
        }
        return saved;
    }
}
