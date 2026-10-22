package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final MessageRuleChain ruleChain;

    public EmailSender(SentMessages sentMessages, MessageRuleChain ruleChain) {
        this.sentMessages = sentMessages;
        this.ruleChain = ruleChain;
    }

    /**
     * Sends an e-mail. The message goes through validation and other rules before being sent.
     * Returns the original message (which may be blocked), or null if redirected in test mode.
     */
    public Message send(String to, String subject, String body) {
        OutgoingMessage outgoing = new OutgoingMessage(to, subject, body);
        List<OutgoingMessage> processed = ruleChain.process(outgoing);

        List<Message> saved = new ArrayList<>();
        for (OutgoingMessage msg : processed) {
            String status = msg.isBlocked() ? "BLOCKED" : "SENT";
            saved.add(sentMessages.add("EMAIL", msg.to(), msg.subject(), msg.body(), status));
        }

        return saved.isEmpty() ? null : saved.get(0);
    }
}
