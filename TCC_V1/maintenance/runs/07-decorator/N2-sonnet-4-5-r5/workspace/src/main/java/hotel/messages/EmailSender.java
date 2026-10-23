package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final FilterChain filterChain;

    public EmailSender(SentMessages sentMessages, FilterChain filterChain) {
        this.sentMessages = sentMessages;
        this.filterChain = filterChain;
    }

    public Message send(String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(to, subject, body);
        FilterResult result = filterChain.apply(outgoing);

        // save the original message (sent or blocked)
        Message saved = sentMessages.add(
                "EMAIL", result.message().to(), result.message().subject(), result.message().body(), result.status());

        // save extra messages (like archive copies) - they are always sent
        for (OutgoingMessage extra : result.extraMessages()) {
            sentMessages.add("EMAIL", extra.to(), extra.subject(), extra.body(), MessageStatus.SENT);
        }

        return saved;
    }
}
