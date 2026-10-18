package hotel.messages;

/** Validates e-mail addresses to prevent sending messages to typos or malformed addresses. */
public final class EmailValidator {

    /**
     * An e-mail address is valid when it has exactly one @, something before it, and after it a name
     * with at least one dot in the middle (like ana@gmail.com).
     */
    public static boolean isValid(String email) {
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
        if (domain.isEmpty()) {
            return false;
        }

        return domain.contains(".");
    }
}
