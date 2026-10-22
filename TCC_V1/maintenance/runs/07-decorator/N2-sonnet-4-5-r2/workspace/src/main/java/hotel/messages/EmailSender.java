package hotel.messages;

import java.util.List;

/** Sends e-mails to guests. Every e-mail sent is kept in the list of sent messages. */
public final class EmailSender {

    private final SentMessages sentMessages;
    private final List<MessageRule> rules;

    public EmailSender(SentMessages sentMessages, List<MessageRule> rules) {
        this.sentMessages = sentMessages;
        this.rules = List.copyOf(rules);
    }

    public Message send(String to, String subject, String body) {
        MessageContext context = new MessageContext(new OutgoingMessage(to, subject, body));

        for (MessageRule rule : rules) {
            rule.apply(context);
        }

        OutgoingMessage finalMessage = context.message();
        Message main = sentMessages.add("EMAIL", finalMessage.to(), finalMessage.subject(),
                finalMessage.body(), context.status());

        for (OutgoingMessage additional : context.additionalMessages()) {
            sentMessages.add("EMAIL", additional.to(), additional.subject(),
                    additional.body(), MessageStatus.SENT);
        }

        return main;
    }
}
