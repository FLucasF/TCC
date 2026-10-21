package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Every message the inn sent, so the front desk can check what each guest received. */
public final class SentMessages {

    private final List<Message> messages = new ArrayList<>();

    synchronized Message add(String channel, String to, String subject, String body, MessageStatus status) {
        Message message = new Message(messages.size() + 1L, channel, to, subject, body, status);
        messages.add(message);
        return message;
    }

    public synchronized List<Message> all() {
        return new ArrayList<>(messages);
    }
}
