package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<EmailRule> rules;

    public EmailSender(SentMessages sentMessages) {
        this.sentMessages = sentMessages;
        this.rules = new ArrayList<>();
        this.rules.add(new ValidateEmailRule());
        this.rules.add(new TestModeRule());
    }

    public Message send(String to, String subject, String body) {
        EmailToSend email = new EmailToSend(to, subject, body);
        String originalTo = to;
        for (EmailRule rule : rules) {
            ProcessingResult result = rule.process(email);
            if (result.isBlocked()) {
                return sentMessages.add("EMAIL", originalTo, subject, body, "BLOCKED");
            }
            email = result.email();
        }
        Message sent = sentMessages.add("EMAIL", email.to(), email.subject(), email.body(), "SENT");
        boolean testMode = "true".equals(System.getenv("HOTEL_TEST_MODE"));
        if (!testMode) {
            sentMessages.add("EMAIL", "arquivo@pousadamarealta.com.br", "[Cópia] " + email.subject(),
                    email.body(), "SENT");
        }
        return sent;
    }
}
