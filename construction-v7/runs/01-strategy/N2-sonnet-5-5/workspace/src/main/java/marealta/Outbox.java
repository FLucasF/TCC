package marealta;

import java.util.ArrayList;
import java.util.List;

/** Lista de mensagens "enviadas"; por enquanto nada sai de verdade. */
public class Outbox {
    private final List<Message> sent = new ArrayList<>();

    public synchronized void sendEmail(String to, String subject, String body) {
        sent.add(new Message(sent.size() + 1, "EMAIL", to, subject, body));
    }

    public synchronized List<Message> sent() {
        return List.copyOf(sent);
    }
}
