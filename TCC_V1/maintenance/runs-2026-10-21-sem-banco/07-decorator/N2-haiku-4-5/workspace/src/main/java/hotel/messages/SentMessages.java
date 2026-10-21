package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Every message the inn sent, so the front desk can check what each guest received. */
public final class SentMessages {

    private final List<Message> messages = new ArrayList<>();
    private final EmailConfiguration config;

    public SentMessages() {
        this(new EmailConfiguration());
    }

    public SentMessages(EmailConfiguration config) {
        this.config = config;
    }

    synchronized Message add(String channel, String to, String subject, String body) {
        MessageStatus status = MessageStatus.BLOCKED;
        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (!EmailValidator.isValid(to)) {
            Message blocked = new Message(nextId(), channel, to, subject, body, MessageStatus.BLOCKED);
            messages.add(blocked);
            return blocked;
        }

        if (config.isTestMode()) {
            actualTo = "testes@pousadamarealta.com.br";
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
            status = MessageStatus.SENT;
        } else {
            status = MessageStatus.SENT;
        }

        Message message = new Message(nextId(), channel, actualTo, actualSubject, actualBody, status);
        messages.add(message);

        if (!config.isTestMode() && config.shouldArchiveCopy()) {
            Message copy = new Message(
                    nextId(),
                    channel,
                    "arquivo@pousadamarealta.com.br",
                    "[Cópia] " + subject,
                    body,
                    MessageStatus.SENT);
            messages.add(copy);
        }

        return message;
    }

    private long nextId() {
        return messages.size() + 1L;
    }

    public synchronized List<Message> all() {
        return new ArrayList<>(messages);
    }
}
