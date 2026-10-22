package hotel.messages;

import java.util.ArrayList;
import java.util.List;

public final class MessageProcessor {

    private static final String ARCHIVE_EMAIL = "arquivo@pousadamarealta.com.br";
    private static final String TEST_EMAIL = "testes@pousadamarealta.com.br";

    private final EmailValidator emailValidator;
    private final boolean testMode;

    public MessageProcessor(EmailValidator emailValidator, boolean testMode) {
        this.emailValidator = emailValidator;
        this.testMode = testMode;
    }

    public List<ProcessedMessage> process(String to, String subject, String body) {
        List<ProcessedMessage> messages = new ArrayList<>();

        if (!emailValidator.isValid(to)) {
            messages.add(new ProcessedMessage(to, subject, body, MessageStatus.BLOCKED));
            return messages;
        }

        if (testMode) {
            String testSubject = "[TESTE] " + subject;
            String testBody = "Destinatário original: " + to + "\n" + body;
            messages.add(new ProcessedMessage(TEST_EMAIL, testSubject, testBody, MessageStatus.SENT));
        } else {
            messages.add(new ProcessedMessage(to, subject, body, MessageStatus.SENT));
            String archiveSubject = "[Cópia] " + subject;
            messages.add(new ProcessedMessage(ARCHIVE_EMAIL, archiveSubject, body, MessageStatus.SENT));
        }

        return messages;
    }
}
