package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. Rules can
 * validate, modify, block, or generate additional messages.
 */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailRule> rules;

    public EmailSender(SentMessages sentMessages, List<EmailRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = rules;
    }

    public Message send(String to, String subject, String body) {
        OutgoingEmail email = new OutgoingEmail(to, subject, body);
        List<OutgoingEmail> additionalEmails = new ArrayList<>();

        for (EmailRule rule : rules) {
            List<OutgoingEmail> generated = rule.process(email);
            additionalEmails.addAll(generated);
        }

        Message primaryMessage = sentMessages.add("EMAIL", email.to(), email.subject(), email.body(), email.status());

        for (OutgoingEmail additional : additionalEmails) {
            sentMessages.add("EMAIL", additional.to(), additional.subject(), additional.body(), additional.status());
        }

        return primaryMessage;
    }
}
