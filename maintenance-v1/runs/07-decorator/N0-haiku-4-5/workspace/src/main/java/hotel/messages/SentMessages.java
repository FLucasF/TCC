package hotel.messages;

import java.util.ArrayList;
import java.util.List;

/** Every message the inn sent, so the front desk can check what each guest received. */
public final class SentMessages {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";
    private static final boolean TEST_MODE = "true".equalsIgnoreCase(System.getenv("HOTEL_TEST_MODE"));

    private final List<Message> messages = new ArrayList<>();

    synchronized Message add(String channel, String to, String subject, String body) {
        if (!isValidEmail(to)) {
            Message blocked = new Message(nextId(), channel, to, subject, body, "BLOCKED");
            messages.add(blocked);
            return blocked;
        }

        String finalTo = to;
        String finalSubject = subject;
        String finalBody = body;

        if (TEST_MODE) {
            finalSubject = "[TESTE] " + subject;
            finalBody = "Destinatário original: " + to + "\n" + body;
            finalTo = TEST_EMAIL;
        }

        Message sent = new Message(nextId(), channel, finalTo, finalSubject, finalBody, "SENT");
        messages.add(sent);

        if (!TEST_MODE) {
            Message copy = new Message(nextId(), channel, ARCHIVE_EMAIL, "[Cópia] " + subject, body, "SENT");
            messages.add(copy);
        }

        return sent;
    }

    private long nextId() {
        return messages.size() + 1L;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atCount = 0;
        int atIndex = -1;
        for (int i = 0; i < email.length(); i++) {
            if (email.charAt(i) == '@') {
                atCount++;
                atIndex = i;
            }
        }
        if (atCount != 1 || atIndex == 0) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.length() > 0 && domain.contains(".");
    }

    public synchronized List<Message> all() {
        return new ArrayList<>(messages);
    }
}
