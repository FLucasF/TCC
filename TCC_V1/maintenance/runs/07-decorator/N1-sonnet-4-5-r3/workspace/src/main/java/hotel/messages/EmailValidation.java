package hotel.messages;

import java.util.List;

/** Blocks messages with invalid e-mail addresses. */
public final class EmailValidation implements SendingRule {

    @Override
    public List<OutgoingMessage> apply(OutgoingMessage message) {
        if (isValid(message.to())) {
            return List.of(message);
        }
        return List.of(message.withStatus(MessageStatus.BLOCKED));
    }

    private static boolean isValid(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);
        if (localPart.isEmpty() || domain.isEmpty()) {
            return false;
        }
        return domain.contains(".");
    }
}
