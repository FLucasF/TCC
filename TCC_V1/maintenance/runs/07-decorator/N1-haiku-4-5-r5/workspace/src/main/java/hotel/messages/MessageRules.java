package hotel.messages;

import java.util.Map;

public final class MessageRules {

    private final boolean testMode;
    private final boolean enableArchiveCopy;
    private final String archiveEmail;
    private final String testEmail;

    public MessageRules(Map<String, String> environment) {
        this.testMode = "true".equalsIgnoreCase(environment.getOrDefault("HOTEL_TEST_MODE", "false"));
        this.enableArchiveCopy = true;
        this.archiveEmail = "arquivo@pousadamarealta.com.br";
        this.testEmail = "testes@pousadamarealta.com.br";
    }

    public boolean isTestMode() {
        return testMode;
    }

    public MessageToSend processMessage(String originalTo, String subject, String body) {
        if (!EmailValidator.isValid(originalTo)) {
            return new MessageToSend(originalTo, subject, body, "BLOCKED", null);
        }

        String recipientEmail = originalTo;
        String processedSubject = subject;
        String processedBody = body;

        if (testMode) {
            recipientEmail = testEmail;
            processedSubject = "[TESTE] " + subject;
            processedBody = "Destinatário original: " + originalTo + "\n" + body;
            return new MessageToSend(recipientEmail, processedSubject, processedBody, "SENT", null);
        } else {
            return new MessageToSend(
                    recipientEmail, processedSubject, processedBody, "SENT", createArchiveCopy(subject, body));
        }
    }

    private ArchiveCopy createArchiveCopy(String subject, String body) {
        if (enableArchiveCopy) {
            return new ArchiveCopy(archiveEmail, "[Cópia] " + subject, body);
        }
        return null;
    }

    public record MessageToSend(
            String recipientEmail, String subject, String body, String status, ArchiveCopy archiveCopy) {}

    public record ArchiveCopy(String recipientEmail, String subject, String body) {}
}
