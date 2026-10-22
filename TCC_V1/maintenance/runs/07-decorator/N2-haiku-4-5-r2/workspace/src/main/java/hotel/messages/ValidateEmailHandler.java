package hotel.messages;

import java.util.List;

/** Validates email addresses. Blocks invalid ones. */
public final class ValidateEmailHandler implements MessageHandler {

    @Override
    public List<MessageToStore> handle(MessageToStore message) {
        if (!isValidEmail(message.to())) {
            return List.of(new MessageToStore(message.to(), message.subject(), message.body(), "BLOCKED"));
        }
        return List.of(message);
    }

    private static boolean isValidEmail(String email) {
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
        if (atCount != 1) {
            return false;
        }
        if (atIndex == 0 || atIndex == email.length() - 1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        int dotCount = 0;
        for (char c : domain.toCharArray()) {
            if (c == '.') {
                dotCount++;
            }
        }
        return dotCount >= 1;
    }
}
