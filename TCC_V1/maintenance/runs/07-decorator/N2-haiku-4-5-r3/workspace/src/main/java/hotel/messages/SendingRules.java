package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class SendingRules {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final boolean testMode;

    public SendingRules(String testModeEnv) {
        this.testMode = "true".equalsIgnoreCase(testModeEnv);
    }

    public List<MessageToSend> resolve(String to, String subject, String body) {
        List<MessageToSend> messages = new ArrayList<>();

        if (!EmailValidator.isValid(to)) {
            messages.add(new MessageToSend(to, subject, body, "BLOCKED"));
            return messages;
        }

        if (testMode) {
            String testBody = "Destinatário original: " + to + "\n" + body;
            messages.add(new MessageToSend(TEST_EMAIL, "[TESTE] " + subject, testBody, "SENT"));
        } else {
            messages.add(new MessageToSend(to, subject, body, "SENT"));
            messages.add(new MessageToSend(ARCHIVE_EMAIL, "[Cópia] " + subject, body, "SENT"));
        }

        return messages;
    }

    public record MessageToSend(String to, String subject, String body, String status) {
    }
}
