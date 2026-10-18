package hotel.messages;

import java.util.Collections;
import java.util.List;

/**
 * Blocks emails with invalid addresses. An address is valid when it has exactly one @, something
 * before it, and after it a name with at least one dot (like ana@gmail.com).
 */
public final class EmailValidationRule implements EmailRule {

    @Override
    public List<OutgoingEmail> process(OutgoingEmail email) {
        if (!isValidEmail(email.to())) {
            email.block();
        }
        return Collections.emptyList();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        long atCount = email.chars().filter(ch -> ch == '@').count();
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
