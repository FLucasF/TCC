package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final EmailValidator validator;
    private final boolean testMode;

    public EmailSender(SentMessages sentMessages) {
        this(sentMessages, false);
    }

    public EmailSender(SentMessages sentMessages, boolean testMode) {
        this.sentMessages = sentMessages;
        this.validator = new EmailValidator();
        this.testMode = testMode;
    }

    public Message send(String to, String subject, String body) {
        List<OutgoingEmail> outgoing = new ArrayList<>();

        if (!validator.isValid(to)) {
            outgoing.add(OutgoingEmail.blocked(to, subject, body));
        } else if (testMode) {
            outgoing.add(OutgoingEmail.sent(
                    "testes@pousadamarealta.com.br",
                    "[TESTE] " + subject,
                    "Destinatário original: " + to + "\n" + body));
        } else {
            outgoing.add(OutgoingEmail.sent(to, subject, body));
            outgoing.add(OutgoingEmail.sent(
                    "arquivo@pousadamarealta.com.br",
                    "[Cópia] " + subject,
                    body));
        }

        Message first = null;
        for (OutgoingEmail email : outgoing) {
            Message recorded = sentMessages.add("EMAIL", email.to(), email.subject(), email.body(), email.status());
            if (first == null) {
                first = recorded;
            }
        }
        return first;
    }
}
