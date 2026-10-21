package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class SentMessages {

    private final List<Message> messages = new ArrayList<>();
    private final EmailConfig config;

    public SentMessages(EmailConfig config) {
        this.config = config;
    }

    synchronized Message add(String channel, String to, String subject, String body) {
        if (!EmailValidator.isValid(to)) {
            return addBlocked(channel, to, subject, body);
        }

        String actualTo = to;
        String actualSubject = subject;
        String actualBody = body;

        if (config.isTestMode()) {
            actualTo = "testes@pousadamarealta.com.br";
            actualSubject = "[TESTE] " + subject;
            actualBody = "Destinatário original: " + to + "\n" + body;
        }

        long nextId = messages.size() + 1L;
        Message message = new Message(nextId, channel, actualTo, actualSubject, actualBody, MessageStatus.SENT);
        messages.add(message);

        if (!config.isTestMode() && config.archiveEmail() != null) {
            long archiveId = messages.size() + 1L;
            Message archiveMessage = new Message(
                    archiveId, channel, config.archiveEmail(), "[Cópia] " + subject, body, MessageStatus.SENT);
            messages.add(archiveMessage);
        }

        return message;
    }

    private Message addBlocked(String channel, String to, String subject, String body) {
        long nextId = messages.size() + 1L;
        Message message = new Message(nextId, channel, to, subject, body, MessageStatus.BLOCKED);
        messages.add(message);
        return message;
    }

    public synchronized List<Message> all() {
        return new ArrayList<>(messages);
    }
}
