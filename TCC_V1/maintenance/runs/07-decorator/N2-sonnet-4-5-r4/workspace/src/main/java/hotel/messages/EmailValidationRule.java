package hotel.messages;

import java.util.List;

/** Blocks messages to invalid e-mail addresses. */
public final class EmailValidationRule implements MessageRule {

    @Override
    public List<OutgoingMessage> process(OutgoingMessage message) {
        if (!isValidEmail(message.to())) {
            message.block();
        }
        return List.of(message);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        long atCount = email.chars().filter(c -> c == '@').count();
        if (atCount != 1) {
            return false;
        }
        int atIndex = email.indexOf('@');
        if (atIndex == 0) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.contains(".");
    }
}
